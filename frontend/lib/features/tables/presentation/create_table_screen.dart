import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:latlong2/latlong.dart';

import '../../../core/errors/api_exception.dart';
import '../data/table_models.dart';
import '../data/table_repository.dart';

class CreateTableScreen extends ConsumerStatefulWidget {
  const CreateTableScreen({
    super.key,
    required this.initialLocation,
  });

  final LatLng initialLocation;

  @override
  ConsumerState<CreateTableScreen> createState() => _CreateTableScreenState();
}

class _CreateTableScreenState extends ConsumerState<CreateTableScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _venueController = TextEditingController();
  late LatLng _selectedLocation;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _selectedLocation = widget.initialLocation;
  }

  @override
  void dispose() {
    _titleController.dispose();
    _venueController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate() || _submitting) return;
    setState(() => _submitting = true);

    try {
      final table = await ref.read(tableRepositoryProvider).create(
            title: _titleController.text.trim(),
            venueName: _venueController.text.trim(),
            latitude: _selectedLocation.latitude,
            longitude: _selectedLocation.longitude,
          );
      if (mounted) Navigator.of(context).pop<TableDetails>(table);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_message(error))),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Masa oluştur')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          children: [
            TextFormField(
              controller: _titleController,
              textInputAction: TextInputAction.next,
              maxLength: 100,
              decoration: const InputDecoration(
                labelText: 'Başlık',
                prefixIcon: Icon(Icons.title),
              ),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Başlık gerekli.'
                  : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _venueController,
              textInputAction: TextInputAction.done,
              maxLength: 100,
              decoration: const InputDecoration(
                labelText: 'Mekân adı',
                prefixIcon: Icon(Icons.storefront_outlined),
              ),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Mekân adı gerekli.'
                  : null,
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Icon(Icons.location_on_outlined),
                const SizedBox(width: 8),
                Text(
                  'Konum',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const Spacer(),
                Text(
                  '${_selectedLocation.latitude.toStringAsFixed(5)}, '
                  '${_selectedLocation.longitude.toStringAsFixed(5)}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
            const SizedBox(height: 10),
            SizedBox(
              height: 320,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: FlutterMap(
                  options: MapOptions(
                    initialCenter: _selectedLocation,
                    initialZoom: 16,
                    minZoom: 4,
                    maxZoom: 18,
                    onTap: (_, point) =>
                        setState(() => _selectedLocation = point),
                  ),
                  children: [
                    TileLayer(
                      urlTemplate:
                          'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                      userAgentPackageName: 'com.masabul.mobile',
                    ),
                    MarkerLayer(
                      markers: [
                        Marker(
                          point: _selectedLocation,
                          width: 44,
                          height: 44,
                          alignment: Alignment.topCenter,
                          child: Icon(
                            Icons.location_pin,
                            size: 44,
                            color: Theme.of(context).colorScheme.primary,
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
              ),
            ),
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: _submitting ? null : _submit,
              icon: _submitting
                  ? const SizedBox.square(
                      dimension: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.add),
              label: const Text('Masayı yayınla'),
            ),
          ],
        ),
      ),
    );
  }
}

String _message(Object error) {
  if (error is DioException && error.error is ApiException) {
    return (error.error! as ApiException).message;
  }
  if (error is ApiException) return error.message;
  return 'Masa oluşturulamadı. Lütfen yeniden deneyin.';
}
