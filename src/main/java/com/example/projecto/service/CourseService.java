package com.example.projecto.service;



import com.example.projecto.model.dto.request.CreateCourseRequest;
import com.example.projecto.model.dto.response.CourseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    CourseResponse createCourse(CreateCourseRequest request);
    Page<CourseResponse> getAllCourses(String keyword, Pageable pageable);
    CourseResponse getCourseById(Long id);
    void enrollStudent(Long courseId, String studentUsername);
    Page<CourseResponse> getMyCourses(String studentUsername, Pageable pageable);
}