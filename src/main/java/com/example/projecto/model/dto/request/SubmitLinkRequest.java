package com.example.projecto.model.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitLinkRequest {
    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "GitHub URL is required")
    private String githubUrl;
}
