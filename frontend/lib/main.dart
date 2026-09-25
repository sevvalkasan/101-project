import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'core/auth/firebase_auth_providers.dart';
import 'firebase_options.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  var firebaseReady = true;
  try {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    ).timeout(const Duration(seconds: 10));
  } on Object catch (error) {
    firebaseReady = false;
    debugPrint('Firebase initialization skipped for local run: $error');
  }

  runApp(
    ProviderScope(
      overrides: [
        firebaseReadyProvider.overrideWithValue(firebaseReady),
      ],
      child: const MasaBulApp(),
    ),
  );
}
