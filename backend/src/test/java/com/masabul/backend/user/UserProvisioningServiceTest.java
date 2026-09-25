package com.masabul.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.masabul.backend.security.VerifiedFirebaseToken;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProvisioningService userProvisioningService;

    @Test
    void returnsExistingUserByFirebaseUid() {
        User existingUser = new User("firebase-1", "+905551112233", "Existing User", null, true);
        when(userRepository.findByFirebaseUid("firebase-1")).thenReturn(Optional.of(existingUser));

        User result = userProvisioningService.findOrCreateUser(new VerifiedFirebaseToken("firebase-1", "+905551112233"));

        assertThat(result).isSameAs(existingUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void provisionsNewUserWithIncompleteProfile() {
        when(userRepository.findByFirebaseUid("firebase-2")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userProvisioningService.findOrCreateUser(new VerifiedFirebaseToken("firebase-2", "+905554445566"));

        assertThat(result.getFirebaseUid()).isEqualTo("firebase-2");
        assertThat(result.getPhone()).isEqualTo("+905554445566");
        assertThat(result.getFullName()).isEqualTo("MasaBul Kullanıcısı");
        assertThat(result.isProfileCompleted()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFirebaseUid()).isEqualTo("firebase-2");
    }

    @Test
    void rejectsTokenWithoutPhoneNumber() {
        when(userRepository.findByFirebaseUid("firebase-3")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProvisioningService.findOrCreateUser(new VerifiedFirebaseToken("firebase-3", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Firebase token does not contain a phone number");

        verify(userRepository, never()).save(any());
    }
}
