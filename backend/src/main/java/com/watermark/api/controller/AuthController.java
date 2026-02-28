package com.watermark.api.controller;

import com.watermark.api.dto.*;
import com.watermark.api.entity.User;
import com.watermark.api.repository.UserRepository;
import com.watermark.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        if (req.getEmail() == null || req.getPassword() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));
        if (userRepository.existsByEmail(req.getEmail()))
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName() != null ? req.getFullName() : "")
                .role(User.Role.USER)
                .build();
        userRepository.save(user);

        return ResponseEntity.ok(buildAuthResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        return userRepository.findByEmail(req.getEmail())
                .filter(u -> passwordEncoder.matches(req.getPassword(), u.getPassword()))
                .map(u -> ResponseEntity.ok(buildAuthResponse(u)))
                .orElse(ResponseEntity.status(401).body(null));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String header) {
        String token = header.replace("Bearer ", "");
        if (!jwtUtil.validate(token))
            return ResponseEntity.status(401).build();
        return userRepository.findById(jwtUtil.getUserId(token))
                .map(u -> ResponseEntity.ok(buildAuthResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AuthResponse buildAuthResponse(User u) {
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(u.getId(), u.getEmail(), u.getRole().name()))
                .userId(u.getId()).email(u.getEmail())
                .fullName(u.getFullName()).role(u.getRole().name())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }
}
