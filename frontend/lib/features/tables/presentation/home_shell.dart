import 'package:firebase_auth/firebase_auth.dart' as firebase_auth;
import 'package:flutter/material.dart';

import '../../auth/data/user_profile.dart';

class HomeShell extends StatelessWidget {
  const HomeShell({super.key, required this.currentUser});

  final UserProfile currentUser;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('MasaBul'),
        actions: [
          IconButton(
            tooltip: 'Çıkış yap',
            onPressed: firebase_auth.FirebaseAuth.instance.signOut,
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text(
            'Hoş geldin, ${currentUser.fullName}',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 12),
          const Text(
              'Yakındaki masalar ve harita görünümü Phase 7 kapsamında eklenecek.'),
        ],
      ),
    );
  }
}
