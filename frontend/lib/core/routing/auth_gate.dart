import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/presentation/phone_sign_in_screen.dart';
import '../../features/auth/presentation/profile_completion_screen.dart';
import '../../features/auth/providers/current_user_provider.dart';
import '../../features/tables/presentation/home_shell.dart';
import '../auth/firebase_auth_providers.dart';

class AuthGate extends ConsumerWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authStateProvider);

    return authState.when(
      loading: () => const _LoadingScreen(),
      error: (error, stackTrace) => _ErrorScreen(message: error.toString()),
      data: (firebaseUser) {
        if (firebaseUser == null) {
          return const PhoneSignInScreen();
        }

        final profile = ref.watch(currentUserProvider);
        return profile.when(
          loading: () => const _LoadingScreen(),
          error: (error, stackTrace) => _ErrorScreen(message: error.toString()),
          data: (user) {
            if (!user.profileCompleted) {
              return ProfileCompletionScreen(initialAvatarUrl: user.avatarUrl);
            }
            return HomeShell(currentUser: user);
          },
        );
      },
    );
  }
}

class _LoadingScreen extends StatelessWidget {
  const _LoadingScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(child: CircularProgressIndicator()),
    );
  }
}

class _ErrorScreen extends StatelessWidget {
  const _ErrorScreen({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(message, textAlign: TextAlign.center),
        ),
      ),
    );
  }
}
