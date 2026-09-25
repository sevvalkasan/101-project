package com.masabul.backend.security;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUserPrincipal implements Authentication {

    private final UUID internalUserId;
    private final String firebaseUid;
    private final String phone;
    private boolean authenticated = true;

    public AuthenticatedUserPrincipal(UUID internalUserId, String firebaseUid, String phone) {
        this.internalUserId = internalUserId;
        this.firebaseUid = firebaseUid;
        this.phone = phone;
    }

    public UUID getInternalUserId() {
        return internalUserId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated;
    }

    @Override
    public String getName() {
        return internalUserId.toString();
    }
}
