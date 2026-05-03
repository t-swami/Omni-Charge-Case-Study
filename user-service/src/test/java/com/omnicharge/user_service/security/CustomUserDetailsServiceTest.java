package com.omnicharge.user_service.security;

import com.omnicharge.user_service.entity.Role;
import com.omnicharge.user_service.entity.User;
import com.omnicharge.user_service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService service;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("loadUserByUsername() - found: returns correct UserDetails")
    void loadUserByUsername_found() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setPassword("encoded_pass");
        user.setFullName("Alice");
        user.setPhone("9876543210");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails userDetails = service.loadUserByUsername("alice");

        assertThat(userDetails.getUsername()).isEqualTo("alice");
        assertThat(userDetails.getPassword()).isEqualTo("encoded_pass");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername() - not found: throws UsernameNotFoundException")
    void loadUserByUsername_notFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("loadUserByUsername() - admin user: returns ROLE_ADMIN authority")
    void loadUserByUsername_adminUser() {
        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPassword("encoded_admin");
        admin.setFullName("Admin");
        admin.setPhone("9000000001");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setActive(true);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        UserDetails userDetails = service.loadUserByUsername("admin");

        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
