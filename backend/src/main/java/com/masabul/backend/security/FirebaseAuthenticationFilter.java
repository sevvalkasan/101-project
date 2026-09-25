package com.masabul.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserProvisioningService;
import com.masabul.backend.web.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseTokenVerifier tokenVerifier;
    private final UserProvisioningService userProvisioningService;
    private final ObjectMapper objectMapper;

    public FirebaseAuthenticationFilter(
        FirebaseTokenVerifier tokenVerifier,
        UserProvisioningService userProvisioningService,
        ObjectMapper objectMapper
    ) {
        this.tokenVerifier = tokenVerifier;
        this.userProvisioningService = userProvisioningService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                String idToken = authorization.substring(7);
                VerifiedFirebaseToken verifiedToken = tokenVerifier.verify(idToken);
                User user = userProvisioningService.findOrCreateUser(verifiedToken);
                AuthenticatedUserPrincipal authentication = new AuthenticatedUserPrincipal(
                    user.getId(),
                    user.getFirebaseUid(),
                    user.getPhone()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (FirebaseTokenVerificationException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                writeUnauthorized(response, request);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
            OffsetDateTime.now(),
            HttpServletResponse.SC_UNAUTHORIZED,
            "Invalid Firebase ID token",
            List.of(),
            request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
