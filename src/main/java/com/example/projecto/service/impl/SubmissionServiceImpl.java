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
    public SubmissionResponse submit(String studentUsername, Long courseId, String githubUrl, MultipartFile file) {

        // BƯỚC 1: Validate — phải có ĐÚNG 1 trong 2 (không cả 2, không thiếu cả 2)
        boolean hasLink = githubUrl != null && !githubUrl.isBlank();
        boolean hasFile = file != null && !file.isEmpty();

        if (!hasLink && !hasFile) {
            throw new InvalidStateException("Must provide either githubUrl or file");
        }
        if (hasLink && hasFile) {
            throw new InvalidStateException("Provide only one: githubUrl OR file, not both");
        }

        // BƯỚC 2: Lấy student, course, submission (logic cũ, dùng chung)
        User student = getUser(studentUsername);
        Course course = getCourse(courseId);
        Submission submission = getOrCreateSubmission(student, course);

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            throw new InvalidStateException("This submission has already been graded");
        }

        // BƯỚC 3: Xử lý theo từng trường hợp
        if (hasLink) {
            // Trường hợp 1: nộp link GitHub
            submission.setReportUrl(githubUrl);
        } else {
            // Trường hợp 2: nộp file → validate type → upload Cloudinary
            if (!ALLOWED_TYPES.contains(file.getContentType())) {
                throw new InvalidStateException("Only PDF and Word documents are allowed");
            }

            try {
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

            } catch (IOException e) {
                log.error("Cloudinary upload failed: {}", e.getMessage());
                throw new RuntimeException("Cloud storage service unavailable. Please try again later.");
            }
        }

        // BƯỚC 4: Set status chung cho cả 2 trường hợp
        submission.setStatus(SubmissionStatus.SUBMITTED);

        log.info("Student '{}' submitted via {} for course '{}'",
                studentUsername, hasLink ? "GitHub link" : "file upload", course.getCourseCode());

        return toResponse(submissionRepository.save(submission));
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