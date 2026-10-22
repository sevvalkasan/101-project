import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../../../core/network/api_client.dart';
import '../data/chat_message.dart';

enum ChatConnectionState { connecting, connected, disconnected }

class ChatSocket {
  ChatSocket({
    required this.tableId,
    required this.idToken,
    required this.onMessage,
    required this.onStateChanged,
    required this.onError,
  });

  final String tableId;
  final String idToken;
  final void Function(ChatMessage message) onMessage;
  final void Function(ChatConnectionState state) onStateChanged;
  final void Function(Object error) onError;

  StompClient? _client;

  void connect() {
    disconnect();
    onStateChanged(ChatConnectionState.connecting);
    final uri = Uri.parse(apiBaseUrl);
    final scheme = uri.scheme == 'https' ? 'wss' : 'ws';
    final socketUri = uri.replace(scheme: scheme, path: '/ws-okey');

    final client = StompClient(
      config: StompConfig(
        url: socketUri.toString(),
        stompConnectHeaders: {'Authorization': 'Bearer $idToken'},
        reconnectDelay: const Duration(seconds: 4),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
        onConnect: (frame) {
          onStateChanged(ChatConnectionState.connected);
          _client?.subscribe(
            destination: '/topic/table/$tableId',
            callback: (frame) {
              final body = frame.body;
              if (body == null) return;
              try {
                onMessage(
                  ChatMessage.fromJson(
                    jsonDecode(body) as Map<String, dynamic>,
                  ),
                );
              } on Object catch (error) {
                onError(error);
              }
            },
          );
        },
        onDisconnect: (frame) =>
            onStateChanged(ChatConnectionState.disconnected),
        onWebSocketError: (dynamic error) =>
            onError(error ?? 'WebSocket bağlantı hatası'),
        onStompError: (frame) => onError(frame.body ?? 'Sohbet hatası'),
      ),
    );
    _client = client;
    client.activate();
  }

  bool get isConnected => _client?.connected == true;

  void send(String message) {
    if (!isConnected) {
      throw StateError('Sohbet bağlantısı hazır değil.');
    }
    _client!.send(
      destination: '/app/chat/$tableId',
      body: jsonEncode({'message': message.trim()}),
    );
  }

  void disconnect() {
    _client?.deactivate();
    _client = null;
  }
}
