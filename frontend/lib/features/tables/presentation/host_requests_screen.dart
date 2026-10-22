import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/errors/api_exception.dart';
import '../data/table_models.dart';
import '../data/table_repository.dart';

class HostRequestsScreen extends ConsumerStatefulWidget {
  const HostRequestsScreen({super.key, required this.initialTable});

  final TableDetails initialTable;

  @override
  ConsumerState<HostRequestsScreen> createState() => _HostRequestsScreenState();
}

class _HostRequestsScreenState extends ConsumerState<HostRequestsScreen> {
  late TableDetails _table;
  List<TableJoinRequest> _requests = const [];
  final Set<String> _responding = {};
  bool _loading = true;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _table = widget.initialTable;
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final repository = ref.read(tableRepositoryProvider);
      final results = await Future.wait<Object?>([
        repository.requests(_table.id),
        repository.myActiveTable(),
      ]);
      if (!mounted) return;
      setState(() {
        _requests = results[0] as List<TableJoinRequest>;
        _table = (results[1] as TableDetails?) ?? _table;
      });
    } catch (error) {
      if (mounted) setState(() => _error = error);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _respond(TableJoinRequest request, bool accept) async {
    setState(() => _responding.add(request.requestId));
    try {
      await ref.read(tableRepositoryProvider).respond(
            requestId: request.requestId,
            accept: accept,
          );
      await _load();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_message(error))),
      );
    } finally {
      if (mounted) setState(() => _responding.remove(request.requestId));
    }
  }

  Future<void> _cancelTable() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Masayı iptal et'),
        content: const Text('Açık masa ve bekleyen istekler kapatılacak.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Vazgeç'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('İptal et'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await ref.read(tableRepositoryProvider).cancel(_table.id);
      if (mounted) Navigator.of(context).pop();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_message(error))),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Masam'),
        actions: [
          if (_table.isOpen)
            IconButton(
              tooltip: 'Masayı iptal et',
              onPressed: _cancelTable,
              icon: const Icon(Icons.cancel_outlined),
            ),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _TableStatus(table: _table),
          const Divider(height: 1),
          Expanded(child: _buildRequests()),
        ],
      ),
    );
  }

  Widget _buildRequests() {
    if (_loading && _requests.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null && _requests.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(_message(_error!), textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: _load,
                icon: const Icon(Icons.refresh),
                label: const Text('Yeniden dene'),
              ),
            ],
          ),
        ),
      );
    }
    if (_requests.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: const [
            SizedBox(height: 150),
            Icon(Icons.group_outlined, size: 48),
            SizedBox(height: 12),
            Center(child: Text('Henüz katılma isteği yok.')),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: _requests.length,
        separatorBuilder: (_, __) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          final request = _requests[index];
          final responding = _responding.contains(request.requestId);
          return Card(
            margin: EdgeInsets.zero,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 12, 12),
              child: Row(
                children: [
                  CircleAvatar(
                    backgroundImage: request.user.avatarUrl == null
                        ? null
                        : NetworkImage(request.user.avatarUrl!),
                    child: request.user.avatarUrl == null
                        ? Text(_initial(request.user.fullName))
                        : null,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          request.user.fullName,
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        Text(_statusLabel(request.status)),
                      ],
                    ),
                  ),
                  if (request.isPending && _table.isOpen) ...[
                    IconButton(
                      tooltip: 'Reddet',
                      onPressed:
                          responding ? null : () => _respond(request, false),
                      icon: const Icon(Icons.close),
                    ),
                    IconButton.filled(
                      tooltip: 'Kabul et',
                      onPressed:
                          responding ? null : () => _respond(request, true),
                      icon: responding
                          ? const SizedBox.square(
                              dimension: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.check),
                    ),
                  ],
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _TableStatus extends StatelessWidget {
  const _TableStatus({required this.table});

  final TableDetails table;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(table.title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 4),
          Text(table.venueName),
          const SizedBox(height: 14),
          Row(
            children: [
              const Icon(Icons.groups_outlined, size: 20),
              const SizedBox(width: 6),
              Text('${table.currentPlayers}/${table.maxPlayers} oyuncu'),
              const Spacer(),
              Text(_tableStatusLabel(table.status)),
            ],
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(
            value: table.currentPlayers / table.maxPlayers,
          ),
        ],
      ),
    );
  }
}

String _statusLabel(String status) {
  return switch (status) {
    'PENDING' => 'Bekliyor',
    'ACCEPTED' => 'Kabul edildi',
    'REJECTED' => 'Reddedildi',
    _ => status,
  };
}

String _tableStatusLabel(String status) {
  return switch (status) {
    'OPEN' => 'Açık',
    'FULL' => 'Dolu',
    'CANCELLED' => 'İptal edildi',
    _ => status,
  };
}

String _message(Object error) {
  if (error is DioException && error.error is ApiException) {
    return (error.error! as ApiException).message;
  }
  if (error is ApiException) return error.message;
  return 'İşlem tamamlanamadı. Lütfen yeniden deneyin.';
}

String _initial(String name) {
  final trimmed = name.trim();
  return trimmed.isEmpty ? '?' : trimmed.characters.first.toUpperCase();
}
