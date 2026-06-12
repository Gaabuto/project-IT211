package com.example.projecto.controller;

import com.example.projecto.model.dto.request.GradeRequest;
import com.example.projecto.model.dto.response.SubmissionResponse;
import com.example.projecto.model.entity.SubmissionStatus;
import com.example.projecto.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LecturerController.class)
public class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubmissionService submissionService;

    private SubmissionResponse mockResponse() {
        return SubmissionResponse.builder()
                .id(1L)
                .score(85.0)
                .feedback("Good work")
                .status(SubmissionStatus.GRADED)
                .studentName("Test Student")
                .courseName("OOP")
                .build();
    }

    @Test
    @DisplayName("LecturerController - Chấm điểm thành công trả về 200")
    void testGradeSubmission_ValidRequest_Returns200() throws Exception {
        GradeRequest request = new GradeRequest();
        request.setSubmissionId(1L);
        request.setScore(85.0);
        request.setFeedback("Good work");

        Mockito.when(submissionService.gradeSubmission(eq("lecturer01"), any(GradeRequest.class)))
                .thenReturn(mockResponse());

        mockMvc.perform(post("/api/v1/lecturer/grades")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(85.0))
                .andExpect(jsonPath("$.data.status").value("GRADED"));
    }

    @Test
    @DisplayName("LecturerController - Thiếu submissionId trả về 400")
    @WithMockUser(username = "lecturer01", roles = "LECTURER")
    void testGradeSubmission_MissingSubmissionId_Returns400() throws Exception {
        GradeRequest request = new GradeRequest();
        request.setScore(85.0);

        mockMvc.perform(post("/api/v1/lecturer/grades")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("LecturerController - Score vượt 100 trả về 400")
    @WithMockUser(username = "lecturer01", roles = "LECTURER")
    void testGradeSubmission_ScoreTooHigh_Returns400() throws Exception {
        GradeRequest request = new GradeRequest();
        request.setSubmissionId(1L);
        request.setScore(150.0);

        mockMvc.perform(post("/api/v1/lecturer/grades")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("LecturerController - Student gọi API Lecturer trả về 403")
    @WithMockUser(username = "student01", roles = "STUDENT")
    void testGetSubmissions_StudentRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/lecturer/courses/1/submissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("LecturerController - Lấy danh sách submission theo course thành công")
    @WithMockUser(username = "lecturer01", roles = "LECTURER")
    void testGetSubmissions_ValidCourseId_Returns200() throws Exception {
        Mockito.when(submissionService.getSubmissionsByCourse(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockResponse())));

        mockMvc.perform(get("/api/v1/lecturer/courses/1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].status").value("GRADED"));
    }
}