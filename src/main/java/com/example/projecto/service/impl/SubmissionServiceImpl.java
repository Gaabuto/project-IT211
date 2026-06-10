package com.example.projecto.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.projecto.model.dto.request.GradeRequest;
import com.example.projecto.model.dto.request.SubmitLinkRequest;
import com.example.projecto.model.dto.response.SubmissionResponse;
import com.example.projecto.model.entity.*;
import com.example.projecto.exception.InvalidStateException;
import com.example.projecto.exception.ResourceNotFoundException;
import com.example.projecto.repository.CourseRepository;
import com.example.projecto.repository.SubmissionRepository;
import com.example.projecto.repository.UserRepository;
import com.example.projecto.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // ──────────────── Student: nộp link GitHub ────────────────

    @Override
    @Transactional
    public SubmissionResponse submitLink(String studentUsername, SubmitLinkRequest request) {
        User student = getUser(studentUsername);
        Course course = getCourse(request.getCourseId());

        Submission submission = getOrCreateSubmission(student, course);

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new InvalidStateException("This submission has already been graded");
        }

        submission.setReportUrl(request.getGithubUrl());
        submission.setStatus(SubmissionStatus.SUBMITTED);

        return toResponse(submissionRepository.save(submission));
    }

    // ──────────────── Student: upload file lên Cloudinary ────────────────

    @Override
    @Transactional
    public SubmissionResponse uploadFile(String studentUsername, Long courseId, MultipartFile file) {
        // Validate file type
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidStateException("Only PDF and Word documents are allowed");
        }

        User student = getUser(studentUsername);
        Course course = getCourse(courseId);
        Submission submission = getOrCreateSubmission(student, course);

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new InvalidStateException("This submission has already been graded");
        }

        try {
            // Upload lên Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "submissions/" + courseId,
                            "resource_type", "raw",
                            "public_id", "student_" + student.getId() + "_" + System.currentTimeMillis()
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            submission.setReportUrl(secureUrl);
            submission.setStatus(SubmissionStatus.SUBMITTED);

            log.info("File uploaded to Cloudinary for student '{}', url: {}", studentUsername, secureUrl);
            return toResponse(submissionRepository.save(submission));

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Cloud storage service unavailable. Please try again later.");
        }
    }

    // ──────────────── Lecturer: chấm điểm (UC-04) ────────────────

    @Override
    @Transactional
    public SubmissionResponse gradeSubmission(String lecturerUsername, GradeRequest request) {
        User lecturer = getUser(lecturerUsername);

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + request.getSubmissionId()));

        // Kiểm tra trạng thái: phải SUBMITTED hoặc LATE mới được chấm
        if (submission.getStatus() == SubmissionStatus.PENDING) {
            throw new InvalidStateException("Cannot grade: student has not submitted yet");
        }

        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setLecturer(lecturer);
        submission.setStatus(SubmissionStatus.GRADED);

        // AOP @AfterReturning sẽ tự log sau khi method này return thành công
        return toResponse(submissionRepository.save(submission));
    }

    @Override
    public Page<SubmissionResponse> getSubmissionsByCourse(Long courseId, Pageable pageable) {
        return submissionRepository.findByCourseId(courseId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<SubmissionResponse> getMySubmissions(String studentUsername, Pageable pageable) {
        User student = getUser(studentUsername);
        return submissionRepository.findByStudentId(student.getId(), pageable)
                .map(this::toResponse);
    }

    // ──────────────── Helpers ────────────────

    private Submission getOrCreateSubmission(User student, Course course) {
        return submissionRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseGet(() -> Submission.builder()
                        .student(student)
                        .course(course)
                        .status(SubmissionStatus.PENDING)
                        .build());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }

    private SubmissionResponse toResponse(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .reportUrl(s.getReportUrl())
                .score(s.getScore())
                .feedback(s.getFeedback())
                .status(s.getStatus())
                .studentName(s.getStudent() != null ? s.getStudent().getFullName() : null)
                .courseName(s.getCourse() != null ? s.getCourse().getCourseName() : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}