package com.masabul.backend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Component;

@Component
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    @Override
    public VerifiedFirebaseToken verify(String idToken) {
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String phone = token.getClaims().get("phone_number") instanceof String phoneNumber ? phoneNumber : null;
            return new VerifiedFirebaseToken(token.getUid(), phone);
        } catch (FirebaseAuthException | IllegalStateException exception) {
            throw new FirebaseTokenVerificationException("Invalid Firebase ID token", exception);
        }
    }
}
