package com.watermark.api.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
}
