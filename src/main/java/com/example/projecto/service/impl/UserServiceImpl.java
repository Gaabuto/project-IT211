package com.example.projecto.service.impl;


import com.example.projecto.model.dto.response.UserResponse;
import com.example.projecto.model.entity.RoleEnum;
import com.example.projecto.model.entity.User;
import com.example.projecto.exception.ResourceNotFoundException;
import com.example.projecto.repository.UserRepository;
import com.example.projecto.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public Page<UserResponse> getAllUsers(String keyword, RoleEnum role, Pageable pageable) {
        // Bắt buộc dùng Stream API để map Entity -> DTO (UC-02)
        return userRepository.findByKeywordAndRole(keyword, role, pageable)
                .map(this::toUserResponse);
    }

    @Override
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setIsActive(isActive);
        return toUserResponse(userRepository.save(user));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build();
    }
}