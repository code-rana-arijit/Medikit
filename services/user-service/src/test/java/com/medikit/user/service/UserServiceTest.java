package com.medikit.user.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.user.dto.RegisterRequest;
import com.medikit.user.dto.UserResponse;
import com.medikit.user.entity.User;
import com.medikit.user.entity.UserRole;
import com.medikit.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@medikit.com")
                .phone("9876543210")
                .fullName("Test User")
                .passwordHash("hash")
                .build();
    }

    @Test
    void getProfile_returnsUserResponse() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile(user.getId());

        assertThat(response.email()).isEqualTo("test@medikit.com");
        assertThat(response.fullName()).isEqualTo("Test User");
    }

    @Test
    void getProfile_throwsWhenUserNotFound() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void upgradeRole_upgradesCustomerToDistributor() {
        user.setRole(UserRole.CUSTOMER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.upgradeRole(user.getId(), "DISTRIBUTOR");

        assertThat(response.role()).isEqualTo("DISTRIBUTOR");
        assertThat(user.getRole()).isEqualTo(UserRole.DISTRIBUTOR);
    }

    @Test
    void upgradeRole_rejectsAdminRole() {
        assertThatThrownBy(() -> userService.upgradeRole(user.getId(), "ADMIN"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void upgradeRole_rejectsDeliveryPartnerRoleChange() {
        user.setRole(UserRole.DELIVERY_PARTNER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.upgradeRole(user.getId(), "DISTRIBUTOR"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void adminSetRole_grantsDeliveryPartner() {
        user.setRole(UserRole.CUSTOMER);
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@medikit.com")
                .fullName("Admin")
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.adminSetRole(admin.getId(), user.getId(), "DELIVERY_PARTNER");

        assertThat(response.role()).isEqualTo("DELIVERY_PARTNER");
        assertThat(user.getRole()).isEqualTo(UserRole.DELIVERY_PARTNER);
    }

    @Test
    void adminSetRole_throwsForbiddenWhenCallerNotAdmin() {
        user.setRole(UserRole.CUSTOMER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.adminSetRole(user.getId(), user.getId(), "DISTRIBUTOR"))
                .isInstanceOf(com.medikit.common.web.ForbiddenException.class);
    }

    @Test
    void adminSetRole_rejectsAssigningAdmin() {
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@medikit.com")
                .fullName("Admin")
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.adminSetRole(admin.getId(), user.getId(), "ADMIN"))
                .isInstanceOf(BadRequestException.class);
    }
}
