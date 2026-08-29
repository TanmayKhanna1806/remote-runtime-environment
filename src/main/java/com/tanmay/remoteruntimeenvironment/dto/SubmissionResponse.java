package com.tanmay.remoteruntimeenvironment.dto;

import com.tanmay.remoteruntimeenvironment.executor.ExecutionResult;

public record SubmissionResponse(
        Long id,
        ExecutionResult.Verdict verdict,
        String output,
        String error,
        long executionTimeMs
) {
}