import 'package:flutter/material.dart';

import 'core/routing/auth_gate.dart';
import 'core/theme/app_theme.dart';

class MasaBulApp extends StatelessWidget {
  const MasaBulApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MasaBul',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: const AuthGate(),
    );
  }
}
