package com.tanmay.remoteruntimeenvironment.controller;

import com.tanmay.remoteruntimeenvironment.dto.SubmissionResponse;
import com.tanmay.remoteruntimeenvironment.model.Submission;
import com.tanmay.remoteruntimeenvironment.model.TestCase;
import com.tanmay.remoteruntimeenvironment.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(
            SubmissionService submissionService) {

        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> submit(
            @RequestBody SubmissionRequest request) {

        Submission submission =
                submissionService.submit(
                        request.sourceCode(),
                        request.testCases()
                );

        return ResponseEntity.accepted().body(
                toResponse(submission)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmission(
            @PathVariable Long id) {

        Submission submission =
                submissionService.getSubmission(id);

        if (submission == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                toResponse(submission)
        );
    }

    private SubmissionResponse toResponse(
            Submission submission) {

        return new SubmissionResponse(
                submission.getId(),
                submission.getVerdict(),
                submission.getOutput(),
                submission.getError(),
                submission.getExecutionTimeMs()
        );
    }

    public record SubmissionRequest(
            String sourceCode,
            List<TestCase> testCases
    ) {
    }
}