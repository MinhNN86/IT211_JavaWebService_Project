package com.project.data;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.RoleName;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Override
    @Transactional
    public void run(String... args) {
        seed("admin", "Administrator", "admin@demo.local", RoleName.ADMIN);
        seed("manager", "Manager", "manager@demo.local", RoleName.MANAGER);
        seed("customer", "Customer", "customer@demo.local", RoleName.CUSTOMER);
    }

    private void seed(String username, String fullName, String email, RoleName role) {
        if (!users.existsByUsername(username))
            users.save(User.builder().username(username).fullName(fullName).email(email)
                    .password(encoder.encode("123456")).role(role).build());
    }
}
