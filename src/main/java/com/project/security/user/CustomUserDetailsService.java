package com.project.security.user;

import java.util.UUID;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import com.project.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDetails(user);
    }

    public UserDetails loadUserById(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDetails(user);
    }

    private UserDetails toUserDetails(com.project.modules.user.entity.User user) {
        return User.withUsername(user.getUsername()).password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .disabled(user.getStatus() != com.project.common.enums.UserStatus.ACTIVE).build();
    }
}
