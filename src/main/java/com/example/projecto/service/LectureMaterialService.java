package com.example.projecto.service;

import com.example.projecto.model.dto.response.LectureMaterialResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface LectureMaterialService {
    LectureMaterialResponse uploadMaterial(String lecturerUsername, Long courseId, MultipartFile file);
    Page<LectureMaterialResponse> getMaterialsByCourse(Long courseId, Pageable pageable);
}