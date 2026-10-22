import 'package:dio/dio.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';

import '../../../core/errors/api_exception.dart';
import '../../auth/data/user_profile.dart';
import '../../chat/presentation/chat_list_screen.dart';
import '../../notifications/application/device_messaging_service.dart';
import '../application/location_service.dart';
import '../data/table_models.dart';
import '../data/table_repository.dart';
import 'create_table_screen.dart';
import 'host_requests_screen.dart';

enum _HomeView { map, list }

class HomeShell extends ConsumerStatefulWidget {
  const HomeShell({super.key, required this.currentUser});

  final UserProfile currentUser;

  @override
  ConsumerState<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends ConsumerState<HomeShell> {
  static const _istanbul = LatLng(41.0082, 28.9784);

  final _mapController = MapController();
  _HomeView _view = _HomeView.map;
  Position? _position;
  List<NearbyTable> _tables = const [];
  TableDetails? _activeTable;
  Object? _error;
  bool _loading = false;
  bool _mapReady = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadNearby();
      ref.read(deviceMessagingServiceProvider).start(
        onForegroundMessage: (message) {
          if (!mounted) return;
          final notification = message.notification;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                notification?.body ?? 'Masa bildirimi aldın.',
              ),
            ),
          );
        },
      );
    });
  }

  @override
  void dispose() {
    ref.read(deviceMessagingServiceProvider).stop();
    super.dispose();
  }

  Future<void> _loadNearby() async {
    if (_loading) return;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final position =
          await ref.read(locationServiceProvider).currentPosition();
      final repository = ref.read(tableRepositoryProvider);
      final results = await Future.wait<Object?>([
        repository.nearby(
          latitude: position.latitude,
          longitude: position.longitude,
        ),
        repository.myActiveTable(),
      ]);
      if (!mounted) return;
      setState(() {
        _position = position;
        _tables = results[0] as List<NearbyTable>;
        _activeTable = results[1] as TableDetails?;
      });
      if (_mapReady) {
        _mapController.move(
          LatLng(position.latitude, position.longitude),
          14,
        );
      }
    } catch (error) {
      if (mounted) setState(() => _error = error);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _openCreateTable() async {
    var position = _position;
    if (position == null) {
      try {
        position = await ref.read(locationServiceProvider).currentPosition();
      } catch (error) {
        if (mounted) _showError(error);
        return;
      }
    }
    if (!mounted) return;

    final created = await Navigator.of(context).push<TableDetails>(
      MaterialPageRoute(
        builder: (_) => CreateTableScreen(
          initialLocation: LatLng(position!.latitude, position.longitude),
        ),
      ),
    );
    if (created != null) {
      setState(() => _activeTable = created);
      await _loadNearby();
    }
  }

  Future<void> _openRequests() async {
    final table = _activeTable;
    if (table == null) return;
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => HostRequestsScreen(initialTable: table),
      ),
    );
    await _loadNearby();
  }

  Future<void> _openChats() async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => ChatListScreen(currentUserId: widget.currentUser.id),
      ),
    );
  }

  Future<void> _signOut() async {
    await ref.read(deviceMessagingServiceProvider).unregister();
    await firebase_auth.FirebaseAuth.instance.signOut();
  }

  void _showError(Object error) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(_errorMessage(error))),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('MasaBul'),
            Text(
              widget.currentUser.fullName,
              style: Theme.of(context).textTheme.labelMedium,
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Sohbetler',
            onPressed: _openChats,
            icon: const Icon(Icons.forum_outlined),
          ),
          if (_activeTable != null)
            IconButton(
              tooltip: 'Masa istekleri',
              onPressed: _openRequests,
              icon: Badge(
                isLabelVisible: _activeTable!.isOpen,
                child: const Icon(Icons.group_outlined),
              ),
            ),
          IconButton(
            tooltip: 'Çıkış yap',
            onPressed: _signOut,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: Column(
        children: [
          _HomeToolbar(
            view: _view,
            loading: _loading,
            tableCount: _tables.length,
            onViewChanged: (view) => setState(() => _view = view),
            onRefresh: _loadNearby,
          ),
          Expanded(child: _buildContent()),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _activeTable == null ? _openCreateTable : _openRequests,
        icon: Icon(_activeTable == null ? Icons.add : Icons.table_restaurant),
        label: Text(_activeTable == null ? 'Masa oluştur' : 'Masam'),
      ),
    );
  }

  Widget _buildContent() {
    final error = _error;
    if (error != null && _position == null) {
      return _ErrorState(
        error: error,
        onRetry: _loadNearby,
        onSettings: error is LocationFailure && error.canOpenSettings
            ? () => ref.read(locationServiceProvider).openSettings(error.type)
            : null,
      );
    }
    if (_loading && _position == null) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_view == _HomeView.list) {
      return _TableList(
        tables: _tables,
        currentUserId: widget.currentUser.id,
        onRefresh: _loadNearby,
        onSelected: _showTable,
      );
    }
    return _buildMap();
  }

  Widget _buildMap() {
    final position = _position;
    final center = position == null
        ? _istanbul
        : LatLng(position.latitude, position.longitude);

    return Stack(
      children: [
        FlutterMap(
          mapController: _mapController,
          options: MapOptions(
            initialCenter: center,
            initialZoom: 14,
            minZoom: 4,
            maxZoom: 18,
            onMapReady: () => _mapReady = true,
          ),
          children: [
            TileLayer(
              urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
              userAgentPackageName: 'com.masabul.mobile',
            ),
            MarkerLayer(
              markers: [
                if (position != null)
                  Marker(
                    point: center,
                    width: 30,
                    height: 30,
                    child: const _CurrentLocationMarker(),
                  ),
                ..._tables.map(
                  (table) => Marker(
                    point: LatLng(table.latitude, table.longitude),
                    width: 132,
                    height: 58,
                    alignment: Alignment.topCenter,
                    child: _TableMarker(
                      table: table,
                      onTap: () => _showTable(table),
                    ),
                  ),
                ),
              ],
            ),
            const RichAttributionWidget(
              attributions: [
                TextSourceAttribution('OpenStreetMap contributors'),
              ],
            ),
          ],
        ),
        if (_loading)
          const Positioned(
            top: 12,
            left: 0,
            right: 0,
            child: Center(child: CircularProgressIndicator()),
          ),
        if (_tables.isEmpty && !_loading)
          const Positioned(
            top: 16,
            left: 20,
            right: 20,
            child: _MapEmptyBanner(),
          ),
      ],
    );
  }

  void _showTable(NearbyTable table) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (_) => _TableSheet(
        table: table,
        isOwner: table.host.id == widget.currentUser.id,
        onJoin: () async {
          try {
            await ref.read(tableRepositoryProvider).sendJoinRequest(table.id);
            if (!mounted) return;
            Navigator.of(context).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Katılma isteğin gönderildi.')),
            );
          } catch (error) {
            if (mounted) _showError(error);
          }
        },
      ),
    );
  }
}

class _HomeToolbar extends StatelessWidget {
  const _HomeToolbar({
    required this.view,
    required this.loading,
    required this.tableCount,
    required this.onViewChanged,
    required this.onRefresh,
  });

  final _HomeView view;
  final bool loading;
  final int tableCount;
  final ValueChanged<_HomeView> onViewChanged;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 8, 10),
      child: Row(
        children: [
          SegmentedButton<_HomeView>(
            segments: const [
              ButtonSegment(
                value: _HomeView.map,
                icon: Icon(Icons.map_outlined),
                label: Text('Harita'),
              ),
              ButtonSegment(
                value: _HomeView.list,
                icon: Icon(Icons.view_list_outlined),
                label: Text('Liste'),
              ),
            ],
            selected: {view},
            onSelectionChanged: (selection) => onViewChanged(selection.first),
          ),
          const Spacer(),
          Text('$tableCount masa'),
          IconButton(
            tooltip: 'Yenile',
            onPressed: loading ? null : onRefresh,
            icon: const Icon(Icons.my_location),
          ),
        ],
      ),
    );
  }
}

class _TableList extends StatelessWidget {
  const _TableList({
    required this.tables,
    required this.currentUserId,
    required this.onRefresh,
    required this.onSelected,
  });

  final List<NearbyTable> tables;
  final String currentUserId;
  final Future<void> Function() onRefresh;
  final ValueChanged<NearbyTable> onSelected;

  @override
  Widget build(BuildContext context) {
    if (tables.isEmpty) {
      return RefreshIndicator(
        onRefresh: onRefresh,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: const [
            SizedBox(height: 160),
            Icon(Icons.table_restaurant_outlined, size: 48),
            SizedBox(height: 12),
            Center(child: Text('Yakında açık masa bulunamadı.')),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 96),
        itemCount: tables.length,
        separatorBuilder: (_, __) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          final table = tables[index];
          return Card(
            margin: EdgeInsets.zero,
            child: ListTile(
              onTap: () => onSelected(table),
              leading: CircleAvatar(
                backgroundImage: table.host.avatarUrl == null
                    ? null
                    : NetworkImage(table.host.avatarUrl!),
                child: table.host.avatarUrl == null
                    ? Text(_initial(table.host.fullName))
                    : null,
              ),
              title: Text(table.title),
              subtitle: Text(
                '${table.venueName} · ${_formatDistance(table.distanceMeters)}\n'
                '${table.host.fullName}',
              ),
              isThreeLine: true,
              trailing: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text('${table.availableSpots} boş'),
                  if (table.host.id == currentUserId) const Text('Senin masan'),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _TableMarker extends StatelessWidget {
  const _TableMarker({required this.table, required this.onTap});

  final NearbyTable table;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return GestureDetector(
      onTap: onTap,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: colors.primaryContainer,
          border: Border.all(color: colors.primary, width: 1.5),
          borderRadius: BorderRadius.circular(8),
          boxShadow: const [
            BoxShadow(
                color: Colors.black26, blurRadius: 5, offset: Offset(0, 2)),
          ],
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                table.venueName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              Text('${table.availableSpots} boş yer'),
            ],
          ),
        ),
      ),
    );
  }
}

class _CurrentLocationMarker extends StatelessWidget {
  const _CurrentLocationMarker();

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.primary,
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white, width: 4),
        boxShadow: const [BoxShadow(color: Colors.black38, blurRadius: 5)],
      ),
    );
  }
}

class _MapEmptyBanner extends StatelessWidget {
  const _MapEmptyBanner();

  @override
  Widget build(BuildContext context) {
    return Material(
      elevation: 2,
      borderRadius: BorderRadius.circular(8),
      child: const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Text(
          'Bu bölgede açık masa yok.',
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}

class _TableSheet extends StatefulWidget {
  const _TableSheet({
    required this.table,
    required this.isOwner,
    required this.onJoin,
  });

  final NearbyTable table;
  final bool isOwner;
  final Future<void> Function() onJoin;

  @override
  State<_TableSheet> createState() => _TableSheetState();
}

class _TableSheetState extends State<_TableSheet> {
  bool _sending = false;

  @override
  Widget build(BuildContext context) {
    final table = widget.table;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(table.title, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Row(
              children: [
                CircleAvatar(
                  backgroundImage: table.host.avatarUrl == null
                      ? null
                      : NetworkImage(table.host.avatarUrl!),
                  child: table.host.avatarUrl == null
                      ? Text(_initial(table.host.fullName))
                      : null,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(table.venueName),
                      Text('Ev sahibi: ${table.host.fullName}'),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 16,
              children: [
                _InfoChip(
                  icon: Icons.near_me_outlined,
                  text: _formatDistance(table.distanceMeters),
                ),
                _InfoChip(
                  icon: Icons.event_seat_outlined,
                  text: '${table.availableSpots} boş yer',
                ),
              ],
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: widget.isOwner || _sending
                  ? null
                  : () async {
                      setState(() => _sending = true);
                      await widget.onJoin();
                      if (mounted) setState(() => _sending = false);
                    },
              icon: _sending
                  ? const SizedBox.square(
                      dimension: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Icon(widget.isOwner ? Icons.check : Icons.person_add_alt_1),
              label: Text(
                  widget.isOwner ? 'Senin masan' : 'Katılma isteği gönder'),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 18),
        const SizedBox(width: 5),
        Text(text),
      ],
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({
    required this.error,
    required this.onRetry,
    this.onSettings,
  });

  final Object error;
  final VoidCallback onRetry;
  final VoidCallback? onSettings;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.location_off_outlined, size: 48),
            const SizedBox(height: 16),
            Text(_errorMessage(error), textAlign: TextAlign.center),
            const SizedBox(height: 20),
            if (onSettings != null)
              OutlinedButton.icon(
                onPressed: onSettings,
                icon: const Icon(Icons.settings_outlined),
                label: const Text('Ayarları aç'),
              ),
            const SizedBox(height: 8),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Yeniden dene'),
            ),
          ],
        ),
      ),
    );
  }
}

String _errorMessage(Object error) {
  if (error is LocationFailure) return error.message;
  if (error is DioException && error.error is ApiException) {
    return (error.error! as ApiException).message;
  }
  if (error is ApiException) return error.message;
  return 'İşlem tamamlanamadı. Lütfen yeniden deneyin.';
}

String _formatDistance(double meters) {
  if (meters < 1000) return '${meters.round()} m';
  return '${(meters / 1000).toStringAsFixed(1)} km';
}

String _initial(String name) {
  final trimmed = name.trim();
  return trimmed.isEmpty ? '?' : trimmed.characters.first.toUpperCase();
}
