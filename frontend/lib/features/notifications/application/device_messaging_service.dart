import 'dart:async';

import 'package:dio/dio.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';

final deviceMessagingServiceProvider = Provider<DeviceMessagingService>((ref) {
  return DeviceMessagingService(ref.watch(dioProvider));
});

class DeviceMessagingService {
  DeviceMessagingService(this._dio);

  final Dio _dio;
  StreamSubscription<String>? _tokenSubscription;
  StreamSubscription<RemoteMessage>? _messageSubscription;

  Future<void> start({
    required void Function(RemoteMessage message) onForegroundMessage,
  }) async {
    await stop();
    _messageSubscription =
        FirebaseMessaging.onMessage.listen(onForegroundMessage);
    try {
      final messaging = FirebaseMessaging.instance;
      final settings = await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
      );
      if (settings.authorizationStatus == AuthorizationStatus.denied) return;

      if (!kIsWeb && defaultTargetPlatform == TargetPlatform.iOS) {
        final apnsToken = await messaging.getAPNSToken();
        if (apnsToken == null) return;
      }

      final token = await messaging.getToken();
      if (token != null) await _register(token);
      _tokenSubscription = messaging.onTokenRefresh.listen(
        (token) => _register(token),
        onError: (Object error) =>
            debugPrint('FCM token refresh failed: $error'),
      );
    } on Object catch (error) {
      debugPrint('FCM registration skipped: $error');
    }
  }

  Future<void> unregister() async {
    try {
      final token = await FirebaseMessaging.instance.getToken();
      if (token != null) {
        await _dio.delete<void>(
          '/api/v1/devices/fcm-token',
          data: _payload(token),
        );
      }
    } on Object catch (error) {
      debugPrint('FCM token could not be removed: $error');
    } finally {
      await stop();
    }
  }

  Future<void> stop() async {
    await _tokenSubscription?.cancel();
    await _messageSubscription?.cancel();
    _tokenSubscription = null;
    _messageSubscription = null;
  }

  Future<void> _register(String token) {
    return _dio.post<void>(
      '/api/v1/devices/fcm-token',
      data: _payload(token),
    );
  }

  Map<String, String> _payload(String token) => {
        'token': token,
        'platform':
            defaultTargetPlatform == TargetPlatform.iOS ? 'IOS' : 'ANDROID',
      };
}
