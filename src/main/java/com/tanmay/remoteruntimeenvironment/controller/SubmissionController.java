package com.tanmay.remoteruntimeenvironment.controller;

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
    public ResponseEntity<Submission> submit(
            @RequestBody SubmissionRequest request) {

        Submission submission =
                submissionService.submit(
                        request.sourceCode(),
                        request.testCases()
                );

        return ResponseEntity.accepted().body(submission);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submission> getSubmission(
            @PathVariable Long id) {

        Submission submission =
                submissionService.getSubmission(id);

        if (submission == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(submission);
    }

    public record SubmissionRequest(
            String sourceCode,
            List<TestCase> testCases
    ) {
    }
}