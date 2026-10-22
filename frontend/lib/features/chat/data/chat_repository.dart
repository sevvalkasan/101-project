import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import 'chat_message.dart';

final chatRepositoryProvider = Provider<ChatRepository>((ref) {
  return ChatRepository(ref.watch(dioProvider));
});

class ChatRepository {
  const ChatRepository(this._dio);

  final Dio _dio;

  Future<List<ChatMessage>> history(String tableId) async {
    final response = await _dio.get<List<dynamic>>(
      '/api/v1/tables/$tableId/messages',
      queryParameters: {'page': 0, 'size': 100},
    );
    return response.data!
        .map((item) => ChatMessage.fromJson(item as Map<String, dynamic>))
        .toList();
  }
}
