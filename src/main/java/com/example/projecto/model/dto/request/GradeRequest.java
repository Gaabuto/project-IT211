package com.example.projecto.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GradeRequest {

    @NotNull(message = "Submission ID is required")
    private Long submissionId;

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be >= 0")
    @Max(value = 100, message = "Score cannot exceed 100")
    private Double score;

    private String feedback;
}
