package com.delivery.RouteX.dto.auth;

import com.delivery.RouteX.model.User;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private User.Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}