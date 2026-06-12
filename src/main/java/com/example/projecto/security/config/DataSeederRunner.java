package com.example.projecto.security.config;

import com.example.projecto.model.entity.*;
import com.example.projecto.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeederRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed() {

        // ──────────── USERS ────────────

        if (!userRepository.existsByUsername("lecturer01")) {
            userRepository.save(User.builder()
                    .username("lecturer01")
                    .passwordHash(passwordEncoder.encode("Lecturer@123"))
                    .email("lecturer01@edu.com")
                    .fullName("Tran Thi B")
                    .role(RoleEnum.LECTURER)
                    .isActive(true)
                    .build());
            log.info(" Seeded: lecturer01 / Lecturer@123");
        }

        if (!userRepository.existsByUsername("student01")) {
            userRepository.save(User.builder()
                    .username("student01")
                    .passwordHash(passwordEncoder.encode("Student@123"))
                    .email("student01@edu.com")
                    .fullName("Nguyen Van A")
                    .role(RoleEnum.STUDENT)
                    .isActive(true)
                    .build());
            log.info(" Seeded: student01 / Student@123");
        }

        if (!userRepository.existsByUsername("student02")) {
            userRepository.save(User.builder()
                    .username("student02")
                    .passwordHash(passwordEncoder.encode("Student@123"))
                    .email("student02@edu.com")
                    .fullName("Le Thi C")
                    .role(RoleEnum.STUDENT)
                    .isActive(true)
                    .build());
            log.info(" Seeded: student02 / Student@123");
        }

        if (!userRepository.existsByUsername("student_inactive")) {
            userRepository.save(User.builder()
                    .username("student_inactive")
                    .passwordHash(passwordEncoder.encode("Student@123"))
                    .email("inactive@edu.com")
                    .fullName("Pham Van D")
                    .role(RoleEnum.STUDENT)
                    .isActive(false)
                    .build());
            log.info(" Seeded: student_inactive (isActive=false)");
        }

        // ──────────── COURSES ────────────

        User lecturer = userRepository.findByUsername("lecturer01").orElseThrow();

        if (!courseRepository.existsByCourseCode("SE101")) {
            courseRepository.save(Course.builder()
                    .courseCode("SE101")
                    .courseName("Software Engineering")
                    .credit(3)
                    .lecturer(lecturer)
                    .build());
            log.info(" Seeded: Course SE101");
        }

        if (!courseRepository.existsByCourseCode("OOP202")) {
            courseRepository.save(Course.builder()
                    .courseCode("OOP202")
                    .courseName("Object Oriented Programming")
                    .credit(3)
                    .lecturer(lecturer)
                    .build());
            log.info(" Seeded: Course OOP202");
        }

        if (!courseRepository.existsByCourseCode("DB303")) {
            courseRepository.save(Course.builder()
                    .courseCode("DB303")
                    .courseName("Database Management")
                    .credit(4)
                    .lecturer(lecturer)
                    .build());
            log.info(" Seeded: Course DB303");


            // ──────────── ENROLLMENTS ────────────

            User student1 = userRepository.findByUsername("student01").orElseThrow();
            User student2 = userRepository.findByUsername("student02").orElseThrow();
            Course se101 = courseRepository.findByCourseCode("SE101").orElseThrow();
            Course oop = courseRepository.findByCourseCode("OOP202").orElseThrow();

            boolean s1InSe101 = se101.getStudents().stream()
                    .anyMatch(s -> s.getId().equals(student1.getId()));
            if (!s1InSe101) {
                se101.getStudents().add(student1);
                courseRepository.save(se101);
                log.info(" Seeded: student01 enrolled in SE101");
            }

            boolean s1InOop = oop.getStudents().stream()
                    .anyMatch(s -> s.getId().equals(student1.getId()));
            if (!s1InOop) {
                oop.getStudents().add(student1);
                courseRepository.save(oop);
                log.info(" Seeded: student01 enrolled in OOP202");
            }

            se101 = courseRepository.findByCourseCode("SE101").orElseThrow();
            boolean s2InSe101 = se101.getStudents().stream()
                    .anyMatch(s -> s.getId().equals(student2.getId()));
            if (!s2InSe101) {
                se101.getStudents().add(student2);
                courseRepository.save(se101);
                log.info(" Seeded: student02 enrolled in SE101");
            }

            // ──────────── SUBMISSIONS ────────────

            se101 = courseRepository.findByCourseCode("SE101").orElseThrow();
            oop = courseRepository.findByCourseCode("OOP202").orElseThrow();

            if (!submissionRepository.existsByStudentIdAndCourseId(student1.getId(), se101.getId())) {
                submissionRepository.save(Submission.builder()
                        .student(student1)
                        .course(se101)
                        .reportUrl("https://github.com/student01/se101-project")
                        .status(SubmissionStatus.SUBMITTED)
                        .build());
                log.info(" Seeded: Submission SUBMITTED — student01/SE101");
            }

            if (!submissionRepository.existsByStudentIdAndCourseId(student1.getId(), oop.getId())) {
                submissionRepository.save(Submission.builder()
                        .student(student1)
                        .course(oop)
                        .reportUrl("https://github.com/student01/oop-project")
                        .score(90.0)
                        .feedback("Excellent work")
                        .lecturer(lecturer)
                        .status(SubmissionStatus.GRADED)
                        .build());
                log.info(" Seeded: Submission GRADED — student01/OOP202");
            }

            if (!submissionRepository.existsByStudentIdAndCourseId(student2.getId(), se101.getId())) {
                submissionRepository.save(Submission.builder()
                        .student(student2)
                        .course(se101)
                        .status(SubmissionStatus.PENDING)
                        .build());
                log.info(" Seeded: Submission PENDING — student02/SE101");
            }

            log.info(" Data seeding completed!");
        }
    }
}