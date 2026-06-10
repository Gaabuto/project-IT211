package com.example.projecto.controller;


import com.example.projecto.model.dto.request.CreateCourseRequest;
import com.example.projecto.model.dto.response.ApiResponse;
import com.example.projecto.model.dto.response.CourseResponse;
import com.example.projecto.model.dto.response.UserResponse;
import com.example.projecto.model.entity.RoleEnum;
import com.example.projecto.service.CourseService;
import com.example.projecto.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CourseService courseService;

    // ──────── User Management ────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleEnum role,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(keyword, role, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved", users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User retrieved", userService.getUserById(id)));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(ApiResponse.ok("User status updated",
                userService.updateUserStatus(id, isActive)));
    }

    // ──────── Course Management ────────

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
        CourseResponse course = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Course created", course));
    }

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> getCourses(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Courses retrieved",
                courseService.getAllCourses(keyword, pageable)));
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Course retrieved",
                courseService.getCourseById(id)));
    }
}
