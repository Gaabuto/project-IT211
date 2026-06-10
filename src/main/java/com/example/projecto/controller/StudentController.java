package com.example.projecto.controller;


import com.example.projecto.model.dto.request.SubmitLinkRequest;
import com.example.projecto.model.dto.response.ApiResponse;
import com.example.projecto.model.dto.response.CourseResponse;
import com.example.projecto.model.dto.response.SubmissionResponse;
import com.example.projecto.service.CourseService;
import com.example.projecto.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentController {

    private final CourseService courseService;
    private final SubmissionService submissionService;

    // FR-06: Đăng ký tham gia khóa học
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<ApiResponse<Void>> enrollCourse(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long courseId) {
        courseService.enrollStudent(courseId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Enrolled successfully"));
    }

    // Xem các khóa học của tôi
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> getMyCourses(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Courses retrieved",
                courseService.getMyCourses(userDetails.getUsername(), pageable)));
    }

    // FR-07: Nộp bài bằng link GitHub
    @PostMapping("/submissions/link")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmitLinkRequest request) {
        SubmissionResponse response = submissionService.submitLink(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Submission saved", response));
    }

    // UC-05: Upload file lên Cloudinary
    @PostMapping("/submissions/upload")
    public ResponseEntity<ApiResponse<SubmissionResponse>> uploadFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("courseId") Long courseId,
            @RequestParam("file") MultipartFile file) {
        SubmissionResponse response = submissionService.uploadFile(
                userDetails.getUsername(), courseId, file);
        return ResponseEntity.ok(ApiResponse.ok("File uploaded and submission saved", response));
    }

    // Xem các bài nộp của tôi
    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<Page<SubmissionResponse>>> getMySubmissions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Submissions retrieved",
                submissionService.getMySubmissions(userDetails.getUsername(), pageable)));
    }
}
