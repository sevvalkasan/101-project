import 'package:flutter_test/flutter_test.dart';
import 'package:masabul/features/chat/data/chat_message.dart';

void main() {
  test('parses chat message and sender', () {
    final message = ChatMessage.fromJson({
      'id': '11111111-1111-1111-1111-111111111111',
      'tableId': '22222222-2222-2222-2222-222222222222',
      'sender': {
        'id': '33333333-3333-3333-3333-333333333333',
        'fullName': 'Sevval Kasan',
        'avatarUrl': null,
      },
      'message': 'Merhaba masa!',
      'createdAt': '2026-10-04T18:30:00Z',
    });

    expect(message.message, 'Merhaba masa!');
    expect(message.sender.fullName, 'Sevval Kasan');
    expect(message.createdAt.isUtc, isTrue);
  });
}
