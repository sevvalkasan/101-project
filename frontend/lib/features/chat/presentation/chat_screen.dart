import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../tables/data/table_models.dart';
import '../application/chat_socket.dart';
import '../data/chat_message.dart';
import '../data/chat_repository.dart';

class ChatScreen extends ConsumerStatefulWidget {
  const ChatScreen({
    super.key,
    required this.table,
    required this.currentUserId,
  });

  final TableDetails table;
  final String currentUserId;

  @override
  ConsumerState<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends ConsumerState<ChatScreen> {
  final _controller = TextEditingController();
  final _scrollController = ScrollController();
  final List<ChatMessage> _messages = [];
  final Set<String> _messageIds = {};
  ChatSocket? _socket;
  ChatConnectionState _connectionState = ChatConnectionState.connecting;
  Object? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    _socket?.disconnect();
    setState(() {
      _loading = true;
      _error = null;
      _connectionState = ChatConnectionState.connecting;
    });
    try {
      final history =
          await ref.read(chatRepositoryProvider).history(widget.table.id);
      final token = await FirebaseAuth.instance.currentUser?.getIdToken();
      if (token == null || token.isEmpty) {
        throw StateError('Oturum doğrulanamadı.');
      }
      if (!mounted) return;
      _messages
        ..clear()
        ..addAll(history);
      _messageIds
        ..clear()
        ..addAll(history.map((message) => message.id));
      setState(() => _loading = false);
      _connect(token);
      _scrollToBottom();
    } on Object catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = error;
        _connectionState = ChatConnectionState.disconnected;
      });
    }
  }

  void _connect(String token) {
    final socket = ChatSocket(
      tableId: widget.table.id,
      idToken: token,
      onMessage: _addMessage,
      onStateChanged: (state) {
        if (mounted) setState(() => _connectionState = state);
      },
      onError: (error) {
        if (!mounted) return;
        setState(() {
          _error = error;
          _connectionState = ChatConnectionState.disconnected;
        });
      },
    );
    _socket = socket;
    socket.connect();
  }

  void _addMessage(ChatMessage message) {
    if (!mounted || !_messageIds.add(message.id)) return;
    setState(() {
      _messages.add(message);
      _messages.sort((a, b) => a.createdAt.compareTo(b.createdAt));
      _error = null;
    });
    _scrollToBottom();
  }

  void _send() {
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    try {
      _socket?.send(text);
      _controller.clear();
    } on Object catch (error) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(error.toString().replaceFirst('Bad state: ', ''))),
      );
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  void dispose() {
    _socket?.disconnect();
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(widget.table.title),
            Text(
              widget.table.venueName,
              style: Theme.of(context).textTheme.labelMedium,
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          _ConnectionBanner(
            state: _connectionState,
            onRetry: _initialize,
          ),
          Expanded(child: _buildMessages()),
          _Composer(
            controller: _controller,
            enabled: _connectionState == ChatConnectionState.connected,
            onSend: _send,
          ),
        ],
      ),
    );
  }

  Widget _buildMessages() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null && _messages.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_outlined, size: 48),
              const SizedBox(height: 16),
              const Text('Sohbet yüklenemedi.'),
              const SizedBox(height: 12),
              FilledButton.tonalIcon(
                onPressed: _initialize,
                icon: const Icon(Icons.refresh),
                label: const Text('Yeniden dene'),
              ),
            ],
          ),
        ),
      );
    }
    if (_messages.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text('İlk mesajı göndererek sohbeti başlat.'),
        ),
      );
    }

    return ListView.builder(
      controller: _scrollController,
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
      itemCount: _messages.length,
      itemBuilder: (context, index) {
        final message = _messages[index];
        return _MessageBubble(
          message: message,
          isMine: message.sender.id == widget.currentUserId,
        );
      },
    );
  }
}

class _ConnectionBanner extends StatelessWidget {
  const _ConnectionBanner({required this.state, required this.onRetry});

  final ChatConnectionState state;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    if (state == ChatConnectionState.connected) return const SizedBox.shrink();
    final disconnected = state == ChatConnectionState.disconnected;
    return Material(
      color: disconnected
          ? Theme.of(context).colorScheme.errorContainer
          : Theme.of(context).colorScheme.surfaceContainerHighest,
      child: SizedBox(
        width: double.infinity,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              if (!disconnected)
                const SizedBox.square(
                  dimension: 16,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              else
                const Icon(Icons.wifi_off, size: 18),
              const SizedBox(width: 10),
              Expanded(
                child:
                    Text(disconnected ? 'Bağlantı kesildi.' : 'Bağlanıyor...'),
              ),
              if (disconnected)
                TextButton(onPressed: onRetry, child: const Text('Yenile')),
            ],
          ),
        ),
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message, required this.isMine});

  final ChatMessage message;
  final bool isMine;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final localTime = message.createdAt.toLocal();
    final time = '${localTime.hour.toString().padLeft(2, '0')}:'
        '${localTime.minute.toString().padLeft(2, '0')}';
    return Align(
      alignment: isMine ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 320),
        child: Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.end,
            textDirection: isMine ? TextDirection.rtl : TextDirection.ltr,
            children: [
              CircleAvatar(
                radius: 16,
                backgroundImage: message.sender.avatarUrl == null
                    ? null
                    : NetworkImage(message.sender.avatarUrl!),
                child: message.sender.avatarUrl == null
                    ? Text(
                        message.sender.fullName.characters.first.toUpperCase())
                    : null,
              ),
              const SizedBox(width: 8),
              Flexible(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: isMine
                        ? colors.primaryContainer
                        : colors.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(12, 9, 12, 7),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (!isMine)
                          Text(
                            message.sender.fullName,
                            style: Theme.of(context).textTheme.labelMedium,
                          ),
                        Text(message.message),
                        const SizedBox(height: 3),
                        Align(
                          alignment: Alignment.centerRight,
                          child: Text(
                            time,
                            style: Theme.of(context).textTheme.labelSmall,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Composer extends StatelessWidget {
  const _Composer({
    required this.controller,
    required this.enabled,
    required this.onSend,
  });

  final TextEditingController controller;
  final bool enabled;
  final VoidCallback onSend;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Material(
        elevation: 8,
        color: Theme.of(context).colorScheme.surface,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 8, 10),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: TextField(
                  controller: controller,
                  enabled: enabled,
                  minLines: 1,
                  maxLines: 4,
                  textInputAction: TextInputAction.newline,
                  decoration: InputDecoration(
                    hintText: enabled ? 'Mesaj yaz' : 'Bağlantı bekleniyor',
                  ),
                ),
              ),
              IconButton(
                tooltip: 'Gönder',
                onPressed: enabled ? onSend : null,
                icon: const Icon(Icons.send),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
