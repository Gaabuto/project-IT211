package com.example.projecto.service.impl;


import com.example.projecto.model.dto.request.CreateCourseRequest;
import com.example.projecto.model.dto.response.CourseResponse;
import com.example.projecto.model.entity.Course;
import com.example.projecto.model.entity.User;
import com.example.projecto.exception.DuplicateResourceException;
import com.example.projecto.exception.InvalidStateException;
import com.example.projecto.exception.ResourceNotFoundException;
import com.example.projecto.repository.CourseRepository;
import com.example.projecto.repository.UserRepository;
import com.example.projecto.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new DuplicateResourceException("Course code already exists: " + request.getCourseCode());
        }

        Course course = Course.builder()
                .courseCode(request.getCourseCode())
                .courseName(request.getCourseName())
                .credit(request.getCredit())
                .build();

        if (request.getLecturerId() != null) {
            User lecturer = userRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));
            course.setLecturer(lecturer);
        }

        return toCourseResponse(courseRepository.save(course));
    }

    @Override
    public Page<CourseResponse> getAllCourses(String keyword, Pageable pageable) {
        return courseRepository.findByKeyword(keyword, pageable)
                .map(this::toCourseResponse);
    }

    @Override
    public CourseResponse getCourseById(Long id) {
        return courseRepository.findById(id)
                .map(this::toCourseResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    @Override
    @Transactional
    public void enrollStudent(Long courseId, String studentUsername) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        boolean alreadyEnrolled = course.getStudents().stream()
                .anyMatch(s -> s.getId().equals(student.getId()));

        if (alreadyEnrolled) {
            throw new DuplicateResourceException("Student already enrolled in this course");
        }

        course.getStudents().add(student);
        courseRepository.save(course);
        log.info("Student '{}' enrolled in course '{}'", studentUsername, course.getCourseCode());
    }

    @Override
    public Page<CourseResponse> getMyCourses(String studentUsername, Pageable pageable) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return courseRepository.findByStudentId(student.getId(), pageable)
                .map(this::toCourseResponse);
    }

    private CourseResponse toCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .credit(course.getCredit())
                .lecturerName(course.getLecturer() != null ? course.getLecturer().getFullName() : null)
                .enrolledCount(course.getStudents() != null ? course.getStudents().size() : 0)
                .build();
    }
}