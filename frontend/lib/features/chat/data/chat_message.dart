import '../../tables/data/table_models.dart';

class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.tableId,
    required this.sender,
    required this.message,
    required this.createdAt,
  });

  final String id;
  final String tableId;
  final TableHost sender;
  final String message;
  final DateTime createdAt;

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    return ChatMessage(
      id: json['id'] as String,
      tableId: json['tableId'] as String,
      sender: TableHost.fromJson(json['sender'] as Map<String, dynamic>),
      message: json['message'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
