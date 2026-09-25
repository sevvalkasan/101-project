import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final firebaseReadyProvider = Provider<bool>((ref) => true);

final firebaseAuthProvider =
    Provider<FirebaseAuth>((ref) => FirebaseAuth.instance);

final authStateProvider = StreamProvider<User?>((ref) {
  if (!ref.watch(firebaseReadyProvider)) {
    return Stream<User?>.value(null);
  }
  return ref.watch(firebaseAuthProvider).authStateChanges();
});
