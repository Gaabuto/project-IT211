package com.example.projecto.model.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist", indexes = {
        @Index(name = "idx_token_string", columnList = "tokenString")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String tokenString;

    @Column(nullable = false)
    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
