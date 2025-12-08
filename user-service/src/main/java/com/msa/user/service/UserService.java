package com.msa.user.service;

import com.msa.user.client.AuthServiceClient;
import com.msa.user.client.AuthUserResponse;
import com.msa.user.domain.UserProfile;
import com.msa.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * User profile business logic, demonstrating two key MSA patterns:
 *
 * 1. Synchronous composition via OpenFeign: getProfile() combines local DB data
 *    with auth-service data to build a complete user view. This is the API
 *    Composition pattern for read operations across service boundaries.
 *
 * 2. Asynchronous event publishing via RabbitMQ: updateProfile() publishes
 *    domain events after writes, enabling eventual consistency with other
 *    services (e.g., notification-service) without tight coupling.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository profileRepository;
    private final AuthServiceClient authServiceClient;
    private final UserEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Long authUserId) {
        // Fetch profile from local database
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseGet(() -> UserProfile.builder()
                        .authUserId(authUserId)
                        .nickname("New User")
                        .build());

        // Fetch auth user info via OpenFeign (synchronous inter-service call).
        // If auth-service is down, the FallbackFactory returns placeholder data
        // so this service degrades gracefully rather than failing entirely.
        AuthUserResponse authUser = authServiceClient.getUserById(authUserId);

        return Map.of(
                "authUserId", authUserId,
                "email", authUser.email(),
                "name", authUser.name(),
                "role", authUser.role(),
                "nickname", profile.getNickname() != null ? profile.getNickname() : "",
                "bio", profile.getBio() != null ? profile.getBio() : "",
                "profileImageUrl", profile.getProfileImageUrl() != null ? profile.getProfileImageUrl() : ""
        );
    }

    @Transactional
    public UserProfile updateProfile(Long authUserId, String nickname, String bio) {
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseGet(() -> UserProfile.builder()
                        .authUserId(authUserId)
                        .build());

        profile.updateProfile(nickname, bio);
        UserProfile saved = profileRepository.save(profile);

        // Publish event asynchronously so notification-service (and any future
        // consumers) can react to the profile change without being called directly.
        eventPublisher.publishProfileUpdated(authUserId, nickname);

        return saved;
    }
}
