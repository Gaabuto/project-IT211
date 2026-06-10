package com.example.projecto.repository;


import com.example.projecto.model.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    boolean existsByCourseCode(String courseCode);

    @Query("SELECT c FROM Course c WHERE " +
            "(:keyword IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Course c JOIN c.students s WHERE s.id = :studentId")
    Page<Course> findByStudentId(@Param("studentId") Long studentId, Pageable pageable);
}