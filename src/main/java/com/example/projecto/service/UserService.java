package com.example.projecto.service;


import com.example.projecto.model.dto.response.UserResponse;
import com.example.projecto.model.entity.RoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUsers(String keyword, RoleEnum role, Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse updateUserStatus(Long id, boolean isActive);
}