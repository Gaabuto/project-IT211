package com.example.projecto.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final DataSeederRunner dataSeederRunner;

    @Bean
    @Order(2)
    public CommandLineRunner seedData() {
        return args -> dataSeederRunner.seed();
    }
}