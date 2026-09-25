import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/auth/firebase_auth_providers.dart';

final phoneAuthControllerProvider =
    StateNotifierProvider.autoDispose<PhoneAuthController, PhoneAuthState>(
        (ref) {
  return PhoneAuthController(ref.watch(firebaseAuthProvider));
});

class PhoneAuthState {
  const PhoneAuthState({
    this.verificationId,
    this.loading = false,
    this.errorMessage,
  });

  final String? verificationId;
  final bool loading;
  final String? errorMessage;

  bool get codeSent => verificationId != null;

  PhoneAuthState copyWith({
    String? verificationId,
    bool? loading,
    String? errorMessage,
    bool clearError = false,
  }) {
    return PhoneAuthState(
      verificationId: verificationId ?? this.verificationId,
      loading: loading ?? this.loading,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
    );
  }
}

class PhoneAuthController extends StateNotifier<PhoneAuthState> {
  PhoneAuthController(this._auth) : super(const PhoneAuthState());

  final FirebaseAuth _auth;

  Future<void> sendCode(String phoneNumber) async {
    state = state.copyWith(loading: true, clearError: true);
    await _auth.verifyPhoneNumber(
      phoneNumber: phoneNumber.trim(),
      verificationCompleted: (credential) async {
        await _auth.signInWithCredential(credential);
        state = state.copyWith(loading: false);
      },
      verificationFailed: (error) {
        state = state.copyWith(
            loading: false,
            errorMessage: error.message ?? 'Doğrulama başarısız oldu.');
      },
      codeSent: (verificationId, resendToken) {
        state = PhoneAuthState(verificationId: verificationId);
      },
      codeAutoRetrievalTimeout: (verificationId) {
        state = state.copyWith(verificationId: verificationId, loading: false);
      },
    );
  }

  Future<void> verifyCode(String smsCode) async {
    final verificationId = state.verificationId;
    if (verificationId == null) {
      state = state.copyWith(errorMessage: 'Önce telefon numaranı doğrula.');
      return;
    }

    state = state.copyWith(loading: true, clearError: true);
    try {
      final credential = PhoneAuthProvider.credential(
        verificationId: verificationId,
        smsCode: smsCode.trim(),
      );
      await _auth.signInWithCredential(credential);
      state = state.copyWith(loading: false);
    } on FirebaseAuthException catch (error) {
      state = state.copyWith(
        loading: false,
        errorMessage: error.message ?? 'Kod doğrulanamadı.',
      );
    }
  }
}
