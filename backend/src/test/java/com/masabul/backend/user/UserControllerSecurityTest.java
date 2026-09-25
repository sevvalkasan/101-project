package com.masabul.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masabul.backend.security.FirebaseAuthenticationFilter;
import com.masabul.backend.security.FirebaseTokenVerificationException;
import com.masabul.backend.security.FirebaseTokenVerifier;
import com.masabul.backend.security.SecurityConfig;
import com.masabul.backend.security.VerifiedFirebaseToken;
import com.masabul.backend.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockBean
    private UserProvisioningService userProvisioningService;

    @MockBean
    private UserService userService;

    @Test
    void missingTokenCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void invalidTokenCannotAccessProtectedEndpoint() throws Exception {
        when(firebaseTokenVerifier.verify("bad-token"))
            .thenThrow(new FirebaseTokenVerificationException("Invalid Firebase ID token"));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Invalid Firebase ID token"));
    }

    @Test
    void expiredTokenCannotAccessProtectedEndpoint() throws Exception {
        when(firebaseTokenVerifier.verify("expired-token"))
            .thenThrow(new FirebaseTokenVerificationException("Invalid Firebase ID token"));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validTokenProvisionsUserAndReturnsCurrentUser() throws Exception {
        User user = user("firebase-1", "+905551112233", "MasaBul Kullanıcısı", null, false);
        when(firebaseTokenVerifier.verify("valid-token"))
            .thenReturn(new VerifiedFirebaseToken("firebase-1", "+905551112233"));
        when(userProvisioningService.findOrCreateUser(any())).thenReturn(user);
        when(userService.getCurrentUser(any())).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firebaseUid").value("firebase-1"))
            .andExpect(jsonPath("$.phone").value("+905551112233"))
            .andExpect(jsonPath("$.fullName").value("MasaBul Kullanıcısı"))
            .andExpect(jsonPath("$.profileCompleted").value(false));

        verify(userProvisioningService).findOrCreateUser(new VerifiedFirebaseToken("firebase-1", "+905551112233"));
    }

    @Test
    void updateCurrentUserCompletesProfileWithoutAcceptingIdentityFields() throws Exception {
        User updatedUser = user("firebase-1", "+905551112233", "Sevval User", "https://example.com/a.png", true);
        when(firebaseTokenVerifier.verify("valid-token"))
            .thenReturn(new VerifiedFirebaseToken("firebase-1", "+905551112233"));
        when(userProvisioningService.findOrCreateUser(any())).thenReturn(updatedUser);
        when(userService.updateCurrentUser(any(), any())).thenReturn(updatedUser);

        String body = objectMapper.writeValueAsString(java.util.Map.of(
            "fullName", "Sevval User",
            "avatarUrl", "https://example.com/a.png",
            "firebaseUid", "attacker",
            "phone", "+111"
        ));

        mockMvc.perform(put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firebaseUid").value("firebase-1"))
            .andExpect(jsonPath("$.phone").value("+905551112233"))
            .andExpect(jsonPath("$.fullName").value("Sevval User"))
            .andExpect(jsonPath("$.profileCompleted").value(true));
    }

    @Test
    void invalidProfileUpdateReturnsValidationError() throws Exception {
        User user = user("firebase-1", "+905551112233", "MasaBul Kullanıcısı", null, false);
        when(firebaseTokenVerifier.verify("valid-token"))
            .thenReturn(new VerifiedFirebaseToken("firebase-1", "+905551112233"));
        when(userProvisioningService.findOrCreateUser(any())).thenReturn(user);

        mockMvc.perform(put("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private User user(String firebaseUid, String phone, String fullName, String avatarUrl, boolean profileCompleted) {
        User user = new User(firebaseUid, phone, fullName, avatarUrl, profileCompleted);
        ReflectionTestUtils.setField(user, "id", java.util.UUID.randomUUID());
        return user;
    }
}
