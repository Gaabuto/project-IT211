package com.example.projecto.service;



import com.example.projecto.model.dto.request.GradeRequest;
import com.example.projecto.model.dto.request.SubmitLinkRequest;
import com.example.projecto.model.dto.response.SubmissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface SubmissionService {
    SubmissionResponse submitLink(String studentUsername, SubmitLinkRequest request);
    SubmissionResponse uploadFile(String studentUsername, Long courseId, MultipartFile file);
    SubmissionResponse gradeSubmission(String lecturerUsername, GradeRequest request);
    Page<SubmissionResponse> getSubmissionsByCourse(Long courseId, Pageable pageable);
    Page<SubmissionResponse> getMySubmissions(String studentUsername, Pageable pageable);
}