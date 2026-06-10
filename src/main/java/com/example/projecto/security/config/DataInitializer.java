package com.example.projecto.security.config;


import com.example.projecto.model.entity.RoleEnum;
import com.example.projecto.model.entity.User;
import com.example.projecto.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdminAccount() {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("Admin@123456"))
                        .email("admin@edu.com")
                        .fullName("System Administrator")
                        .role(RoleEnum.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);
                log.info("✅ Default admin created: username=admin | password=Admin@123456");
                log.warn("⚠️  SECURITY: Change the default admin password immediately!");
            } else {
                log.info("✅ Admin account already exists, skipping initialization.");
            }
        };
    }
}