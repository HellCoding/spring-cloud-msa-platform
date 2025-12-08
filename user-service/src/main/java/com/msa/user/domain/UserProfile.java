package com.msa.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User profile entity owned by this service's database.
 *
 * authUserId links to the auth-service's AuthUser but is NOT a foreign key,
 * because each service has its own database (database-per-service pattern).
 * Consistency between the two is maintained through eventual consistency
 * via RabbitMQ events, not distributed transactions.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long authUserId;

    private String nickname;

    private String bio;

    private String profileImageUrl;

    private LocalDateTime updatedAt;

    @Builder
    public UserProfile(Long authUserId, String nickname, String bio, String profileImageUrl) {
        this.authUserId = authUserId;
        this.nickname = nickname;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String nickname, String bio) {
        this.nickname = nickname;
        this.bio = bio;
        this.updatedAt = LocalDateTime.now();
    }
}
