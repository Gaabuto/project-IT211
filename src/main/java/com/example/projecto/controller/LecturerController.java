package com.example.projecto.controller;


import com.example.projecto.model.dto.request.GradeRequest;
import com.example.projecto.model.dto.response.ApiResponse;
import com.example.projecto.model.dto.response.LectureMaterialResponse;
import com.example.projecto.model.dto.response.SubmissionResponse;
import com.example.projecto.service.LectureMaterialService;
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
@RequestMapping("/api/v1/lecturer")
@PreAuthorize("hasRole('LECTURER')")
@RequiredArgsConstructor
public class LecturerController {

    private final SubmissionService submissionService;
    private final LectureMaterialService lectureMaterialService;

    // FR-08: Chấm điểm & Ghi nhận xét (UC-04)
    @PostMapping("/grades")
    public ResponseEntity<ApiResponse<SubmissionResponse>> gradeSubmission(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GradeRequest request) {
        SubmissionResponse response = submissionService.gradeSubmission(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Graded successfully", response));
    }

    // Xem danh sách bài nộp theo khóa học
    @GetMapping("/courses/{courseId}/submissions")
    public ResponseEntity<ApiResponse<Page<SubmissionResponse>>> getSubmissions(
            @PathVariable Long courseId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SubmissionResponse> submissions = submissionService.getSubmissionsByCourse(courseId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Submissions retrieved", submissions));
    }

    @PostMapping("/courses/{courseId}/materials")
    public ResponseEntity<ApiResponse<LectureMaterialResponse>> uploadMaterial(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {
        LectureMaterialResponse response = lectureMaterialService.uploadMaterial(
                userDetails.getUsername(), courseId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Material uploaded", response));
    }

    // Xem tài liệu theo khóa học
    @GetMapping("/courses/{courseId}/materials")
    public ResponseEntity<ApiResponse<Page<LectureMaterialResponse>>> getMaterials(
            @PathVariable Long courseId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Materials retrieved",
                lectureMaterialService.getMaterialsByCourse(courseId, pageable)));
    }
}