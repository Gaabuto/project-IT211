package com.example.projecto.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateCourseRequest {

    @NotBlank(message = "Course code is required")
    @Size(max = 20)
    private String courseCode;

    @NotBlank(message = "Course name is required")
    @Size(max = 200)
    private String courseName;

    @NotNull(message = "Credit is required")
    @Min(1) @Max(10)
    private Integer credit;

    private Long lecturerId;
}