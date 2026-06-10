//package com.example.projecto.controller;
//
//import com.example.projecto.model.dto.request.GradeRequest;
//import com.example.projecto.model.dto.response.ApiResponse;
//import com.example.projecto.model.dto.response.SubmissionResponse;
//import com.example.projecto.model.entity.SubmissionStatus;
//import com.example.projecto.service.SubmissionService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(LecturerController.class)
//class SubmissionControllerTest {
//
//    @Autowired MockMvc mockMvc;
//    @Autowired ObjectMapper objectMapper;
//    @MockBean SubmissionService submissionService;
//
//    private SubmissionResponse mockResponse() {
//        return SubmissionResponse.builder()
//                .id(1L)
//                .score(85.0)
//                .feedback("Good work")
//                .status(SubmissionStatus.GRADED)
//                .studentName("Test Student")
//                .courseName("OOP")
//                .build();
//    }
//
//    // Test 6: Chấm điểm thành công
//    @Test
//    @WithMockUser(username = "lecturer01", roles = "LECTURER")
//    void gradeSubmission_validRequest_returns200() throws Exception {
//        GradeRequest request = new GradeRequest();
//        request.setSubmissionId(1L);
//        request.setScore(85.0);
//        request.setFeedback("Good work");
//
//        when(submissionService.gradeSubmission(eq("lecturer01"), any(GradeRequest.class)))
//                .thenReturn(mockResponse());
//
//        mockMvc.perform(post("/api/v1/lecturer/grades")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.score").value(85.0))
//                .andExpect(jsonPath("$.data.status").value("GRADED"));
//    }
//
//    // Test 7: Chấm điểm thiếu submissionId → validation fail
//    @Test
//    @WithMockUser(username = "lecturer01", roles = "LECTURER")
//    void gradeSubmission_missingSubmissionId_returns400() throws Exception {
//        GradeRequest request = new GradeRequest();
//        request.setScore(85.0); // không có submissionId
//
//        mockMvc.perform(post("/api/v1/lecturer/grades")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    // Test 8: Chấm điểm score vượt 100 → validation fail
//    @Test
//    @WithMockUser(username = "lecturer01", roles = "LECTURER")
//    void gradeSubmission_scoreTooHigh_returns400() throws Exception {
//        GradeRequest request = new GradeRequest();
//        request.setSubmissionId(1L);
//        request.setScore(150.0); // > 100
//
//        mockMvc.perform(post("/api/v1/lecturer/grades")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    // Test 9: Xem submissions không có role LECTURER → 403
//    @Test
//    @WithMockUser(username = "student01", roles = "STUDENT")
//    void getSubmissions_studentRole_returns403() throws Exception {
//        mockMvc.perform(get("/api/v1/lecturer/courses/1/submissions"))
//                .andExpect(status().isForbidden());
//    }
//
//    // Test 10: Xem submissions theo courseId thành công
//    @Test
//    @WithMockUser(username = "lecturer01", roles = "LECTURER")
//    void getSubmissions_validCourseId_returns200() throws Exception {
//        when(submissionService.getSubmissionsByCourse(eq(1L), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(List.of(mockResponse())));
//
//        mockMvc.perform(get("/api/v1/lecturer/courses/1/submissions"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.content[0].status").value("GRADED"));
//    }
//}