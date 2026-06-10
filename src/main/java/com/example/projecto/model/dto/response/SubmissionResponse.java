package com.example.projecto.model.dto.response;

import com.example.projecto.model.entity.SubmissionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private String reportUrl;
    private Double score;
    private String feedback;
    private SubmissionStatus status;
    private String studentName;
    private String courseName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
