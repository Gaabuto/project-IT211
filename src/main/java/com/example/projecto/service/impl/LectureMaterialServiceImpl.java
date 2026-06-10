package com.example.projecto.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.projecto.exception.InvalidStateException;
import com.example.projecto.exception.ResourceNotFoundException;
import com.example.projecto.model.dto.response.LectureMaterialResponse;
import com.example.projecto.model.entity.Course;
import com.example.projecto.model.entity.LectureMaterial;
import com.example.projecto.model.entity.User;
import com.example.projecto.repository.CourseRepository;
import com.example.projecto.repository.LectureMaterialRepository;
import com.example.projecto.repository.UserRepository;
import com.example.projecto.service.LectureMaterialService;
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
public class LectureMaterialServiceImpl implements LectureMaterialService {

    private final LectureMaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    @Override
    @Transactional
    public LectureMaterialResponse uploadMaterial(String lecturerUsername, Long courseId, MultipartFile file) {
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidStateException("Only PDF, Word, and PowerPoint files are allowed");
        }

        User lecturer = userRepository.findByUsername(lecturerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "materials/" + courseId,
                            "resource_type", "raw",
                            "public_id", "material_" + System.currentTimeMillis()
                    )
            );

            String url = (String) result.get("secure_url");

            LectureMaterial material = LectureMaterial.builder()
                    .fileName(file.getOriginalFilename())
                    .fileUrl(url)
                    .course(course)
                    .uploadedBy(lecturer)
                    .build();

            log.info("Lecturer '{}' uploaded material '{}' for course '{}'",
                    lecturerUsername, file.getOriginalFilename(), course.getCourseCode());

            return toResponse(materialRepository.save(material));

        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    @Override
    public Page<LectureMaterialResponse> getMaterialsByCourse(Long courseId, Pageable pageable) {
        return materialRepository.findByCourseId(courseId, pageable)
                .map(this::toResponse);
    }

    private LectureMaterialResponse toResponse(LectureMaterial m) {
        return LectureMaterialResponse.builder()
                .id(m.getId())
                .fileName(m.getFileName())
                .fileUrl(m.getFileUrl())
                .courseName(m.getCourse().getCourseName())
                .uploadedBy(m.getUploadedBy().getFullName())
                .uploadedAt(m.getUploadedAt())
                .build();
    }
}