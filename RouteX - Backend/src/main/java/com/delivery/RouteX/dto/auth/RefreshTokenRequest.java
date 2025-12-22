package com.delivery.RouteX.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.* ;

@Data
@NoArgsConstructor @AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}