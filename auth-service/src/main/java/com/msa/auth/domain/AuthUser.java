package com.msa.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Core authentication entity storing user credentials.
 *
 * Deliberately kept separate from UserProfile (in user-service) to follow
 * the database-per-service pattern. Auth concerns (password, role) live here;
 * profile concerns (nickname, bio) live in user-service. This separation
 * ensures that a breach in one service's database doesn't expose the other's data.
 */
@Entity
@Table(name = "auth_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Role {
        USER, ADMIN
    }

    @Builder
    public AuthUser(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role != null ? role : Role.USER;
        this.createdAt = LocalDateTime.now();
    }
}
