package com.msa.user.controller;

import com.msa.user.domain.UserProfile;
import com.msa.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User profile REST endpoints.
 *
 * Mapped under /users/** which the API gateway routes with JWT validation
 * and circuit breaker protection. The X-User-Id header (set by gateway)
 * could be used here for authorization checks in a production scenario.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserProfile> updateProfile(
            @PathVariable("id") Long id,
            @RequestBody UpdateProfileRequest request) {
        UserProfile updated = userService.updateProfile(id, request.nickname(), request.bio());
        return ResponseEntity.ok(updated);
    }

    record UpdateProfileRequest(String nickname, String bio) {}
}
