import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/user_repository.dart';
import '../providers/current_user_provider.dart';

class ProfileCompletionScreen extends ConsumerStatefulWidget {
  const ProfileCompletionScreen({super.key, this.initialAvatarUrl});

  final String? initialAvatarUrl;

  @override
  ConsumerState<ProfileCompletionScreen> createState() =>
      _ProfileCompletionScreenState();
}

class _ProfileCompletionScreenState
    extends ConsumerState<ProfileCompletionScreen> {
  final _nameController = TextEditingController();
  late final TextEditingController _avatarController;
  bool _loading = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _avatarController = TextEditingController(text: widget.initialAvatarUrl);
  }

  @override
  void dispose() {
    _nameController.dispose();
    _avatarController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Profil')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const SizedBox(height: 48),
            Text(
              'Profilini tamamla',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _nameController,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(labelText: 'Ad Soyad'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _avatarController,
              keyboardType: TextInputType.url,
              decoration: const InputDecoration(labelText: 'Avatar URL'),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _loading ? null : _save,
              child: _loading
                  ? const CircularProgressIndicator()
                  : const Text('Kaydet'),
            ),
            if (_errorMessage != null) ...[
              const SizedBox(height: 16),
              Text(
                _errorMessage!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Future<void> _save() async {
    final fullName = _nameController.text.trim();
    if (fullName.isEmpty) {
      setState(() => _errorMessage = 'Ad soyad gerekli.');
      return;
    }

    setState(() {
      _loading = true;
      _errorMessage = null;
    });

    try {
      await ref.read(userRepositoryProvider).updateProfile(
            fullName: fullName,
            avatarUrl: _avatarController.text.trim().isEmpty
                ? null
                : _avatarController.text.trim(),
          );
      ref.invalidate(currentUserProvider);
    } catch (error) {
      setState(() => _errorMessage = error.toString());
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }
}
