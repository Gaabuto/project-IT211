package com.example.projecto.aspect;


import com.example.projecto.model.dto.request.GradeRequest;
import com.example.projecto.model.dto.response.SubmissionResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // ────────── Log tất cả request vào Controller ──────────
    @Before("execution(* com.example.projecto.controller.*.*(..))")
    public void logRequest(JoinPoint jp) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "anonymous";
        log.info("[REQUEST] {} | User: {} | Args: {}",
                jp.getSignature().toShortString(),
                username,
                jp.getArgs());
    }

    @Around("execution(* com.example.projecto.service..*(..))" +
            " || execution(* com.example.projecto.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long elapsed = System.currentTimeMillis() - start;

        log.info("[PERF] {} | {}ms",
                pjp.getSignature().toShortString(),
                elapsed);

        return result;
    }

    // ────────── UC-04: Log sau khi chấm điểm thành công (@AfterReturning) ──────────
    @AfterReturning(
            pointcut = "execution(* com.example.projecto.service.impl.SubmissionServiceImpl.gradeSubmission(..))",
            returning = "result"
    )
    public void logGradingSuccess(JoinPoint jp, Object result) {
        Object[] args = jp.getArgs();
        String lecturerUsername = (String) args[0];
        GradeRequest gradeRequest = (GradeRequest) args[1];
        SubmissionResponse response = (SubmissionResponse) result;

        log.info("[GRADING] Lecturer '{}' graded Submission ID: {} with Score: {} | Status: GRADED",
                lecturerUsername,
                gradeRequest.getSubmissionId(),
                gradeRequest.getScore());
    }

    // ────────── Log khi gradeSubmission ném exception (@AfterThrowing) ──────────
    @AfterThrowing(
            pointcut = "execution(* com.example.projecto.service.impl.SubmissionServiceImpl.gradeSubmission(..))",
            throwing = "ex"
    )
    public void logGradingFailure(JoinPoint jp, Throwable ex) {
        Object[] args = jp.getArgs();
        String lecturerUsername = (String) args[0];
        GradeRequest gradeRequest = (GradeRequest) args[1];

        log.error("[GRADING FAILED] Lecturer '{}' failed to grade Submission ID: {} | Error: {}",
                lecturerUsername,
                gradeRequest != null ? gradeRequest.getSubmissionId() : "unknown",
                ex.getMessage());
    }

    // ────────── Log khi upload file ──────────
    @AfterReturning(
            pointcut = "execution(* com.example.projecto.service.impl.SubmissionServiceImpl.uploadFile(..))",
            returning = "result"
    )
    public void logFileUpload(JoinPoint jp, Object result) {
        String username = (String) jp.getArgs()[0];
        Long courseId   = (Long)   jp.getArgs()[1];
        log.info("[UPLOAD] Student '{}' uploaded file for Course ID: {}", username, courseId);
    }

    // ────────── Log toàn bộ exception từ Service layer ──────────
    @AfterThrowing(
            pointcut = "execution(* com.example.projecto.service..*(..))",
            throwing = "ex"
    )
    public void logServiceException(JoinPoint jp, Throwable ex) {
        log.error("[SERVICE EXCEPTION] {} | Error: {}",
                jp.getSignature().toShortString(),
                ex.getMessage());
    }
}