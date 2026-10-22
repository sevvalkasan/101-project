import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';

final locationServiceProvider = Provider<LocationService>((ref) {
  return const LocationService();
});

class LocationService {
  const LocationService();

  Future<Position> currentPosition() async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      throw const LocationFailure(
        LocationFailureType.serviceDisabled,
        'Konum servisleri kapalı. Ayarlardan konumu açıp yeniden deneyin.',
      );
    }

    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }

    if (permission == LocationPermission.denied) {
      throw const LocationFailure(
        LocationFailureType.permissionDenied,
        'Konum izni verilmedi. Yakındaki masaları gösterebilmek için izin gerekli.',
      );
    }
    if (permission == LocationPermission.deniedForever) {
      throw const LocationFailure(
        LocationFailureType.permissionDeniedForever,
        'Konum izni kalıcı olarak kapalı. Uygulama ayarlarından izin verebilirsiniz.',
      );
    }

    try {
      return await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 12),
        ),
      );
    } on TimeoutException {
      throw const LocationFailure(
        LocationFailureType.timeout,
        'Konum zamanında alınamadı. Açık bir alanda yeniden deneyin.',
      );
    } on LocationServiceDisabledException {
      throw const LocationFailure(
        LocationFailureType.serviceDisabled,
        'Konum servisleri kapalı. Ayarlardan konumu açıp yeniden deneyin.',
      );
    }
  }

  Future<void> openSettings(LocationFailureType type) async {
    if (type == LocationFailureType.serviceDisabled) {
      await Geolocator.openLocationSettings();
    } else {
      await Geolocator.openAppSettings();
    }
  }
}

enum LocationFailureType {
  permissionDenied,
  permissionDeniedForever,
  serviceDisabled,
  timeout,
}

class LocationFailure implements Exception {
  const LocationFailure(this.type, this.message);

  final LocationFailureType type;
  final String message;

  bool get canOpenSettings =>
      type == LocationFailureType.permissionDeniedForever ||
      type == LocationFailureType.serviceDisabled;

  @override
  String toString() => message;
}
