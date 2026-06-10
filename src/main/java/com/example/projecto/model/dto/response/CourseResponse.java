package com.example.projecto.model.dto.response;


import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseResponse {
    private Long id;
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String lecturerName;
    private Integer enrolledCount;
}