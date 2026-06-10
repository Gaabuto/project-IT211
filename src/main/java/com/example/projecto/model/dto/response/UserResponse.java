package com.example.projecto.model.dto.response;


import com.example.projecto.model.entity.RoleEnum;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private RoleEnum role;
    private Boolean isActive;
}