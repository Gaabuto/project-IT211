package com.example.projecto.repository;

import com.example.projecto.model.entity.LectureMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureMaterialRepository extends JpaRepository<LectureMaterial, Long> {
    Page<LectureMaterial> findByCourseId(Long courseId, Pageable pageable);
}