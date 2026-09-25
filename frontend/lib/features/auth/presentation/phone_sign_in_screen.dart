import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/auth/firebase_auth_providers.dart';
import '../application/phone_auth_controller.dart';

class PhoneSignInScreen extends ConsumerStatefulWidget {
  const PhoneSignInScreen({super.key});

  @override
  ConsumerState<PhoneSignInScreen> createState() => _PhoneSignInScreenState();
}

class _PhoneSignInScreenState extends ConsumerState<PhoneSignInScreen> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();

  @override
  void dispose() {
    _phoneController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final firebaseReady = ref.watch(firebaseReadyProvider);
    final state = firebaseReady
        ? ref.watch(phoneAuthControllerProvider)
        : const PhoneAuthState();
    final controller =
        firebaseReady ? ref.read(phoneAuthControllerProvider.notifier) : null;

    return Scaffold(
      appBar: AppBar(title: const Text('MasaBul')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const SizedBox(height: 48),
            Text(
              'Telefon numaranla giriş yap',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 24),
            if (!firebaseReady) ...[
              Text(
                'Firebase yapılandırması henüz eklenmedi. Lokal simülasyon açık; telefon doğrulama için flutterfire configure gerekli.',
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
              const SizedBox(height: 16),
            ],
            TextField(
              controller: _phoneController,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: 'Telefon',
                hintText: '+905551112233',
              ),
            ),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: !firebaseReady || state.loading
                  ? null
                  : () => controller!.sendCode(_phoneController.text),
              child: state.loading && !state.codeSent
                  ? const CircularProgressIndicator()
                  : const Text('Kod gönder'),
            ),
            if (state.codeSent) ...[
              const SizedBox(height: 24),
              TextField(
                controller: _codeController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'SMS kodu'),
              ),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: !firebaseReady || state.loading
                    ? null
                    : () => controller!.verifyCode(_codeController.text),
                child: state.loading
                    ? const CircularProgressIndicator()
                    : const Text('Giriş yap'),
              ),
            ],
            if (state.errorMessage != null) ...[
              const SizedBox(height: 16),
              Text(
                state.errorMessage!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
