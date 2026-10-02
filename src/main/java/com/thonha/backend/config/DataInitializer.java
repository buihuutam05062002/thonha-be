package com.thonha.backend.config;

import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.User;
import com.thonha.backend.entity.UserStatus;
import com.thonha.backend.repository.RoleRepository;
import com.thonha.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Tao san tai khoan ADMIN (va cac role neu thieu) khi khoi dong. */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedAdmin(UserRepository users,
                                RoleRepository roles,
                                PasswordEncoder encoder,
                                @Value("${app.admin.email}") String email,
                                @Value("${app.admin.password}") String password,
                                @Value("${app.admin.full-name:Administrator}") String fullName) {
        return args -> {
            for (String r : new String[]{Role.ADMIN, Role.CUSTOMER, Role.WORKER}) {
                if (roles.findByName(r).isEmpty()) roles.save(new Role(r));
            }
            if (users.existsByEmailIgnoreCase(email)) return;
            User u = new User();
            u.setFullName(fullName);
            u.setEmail(email);
            u.setPassword(encoder.encode(password));
            u.setStatus(UserStatus.ACTIVE);
            u.getRoles().add(roles.findByName(Role.ADMIN).orElseThrow());
            users.save(u);
        };
    }
}
