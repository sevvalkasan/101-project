package com.masabul.backend.security;

public interface FirebaseTokenVerifier {

    VerifiedFirebaseToken verify(String idToken);
}
