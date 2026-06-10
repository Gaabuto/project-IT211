package com.example.projecto.model.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LectureMaterialResponse {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String courseName;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}