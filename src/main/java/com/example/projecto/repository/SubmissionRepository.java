package com.example.projecto.repository;


import com.example.projecto.model.entity.Submission;
import com.example.projecto.model.entity.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Page<Submission> findByStudentId(Long studentId, Pageable pageable);
    Page<Submission> findByCourseId(Long courseId, Pageable pageable);
    Page<Submission> findByCourseIdAndStatus(Long courseId, SubmissionStatus status, Pageable pageable);
    Optional<Submission> findByStudentIdAndCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    List<Submission> findByStatus(SubmissionStatus status);
}
