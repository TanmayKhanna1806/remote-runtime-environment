package com.tanmay.remoteruntimeenvironment.executor;

public class ExecutionResult {

    public enum Verdict {
        ACCEPTED,
        WRONG_ANSWER,
        COMPILE_ERROR,
        RUNTIME_ERROR,
        TIME_LIMIT_EXCEEDED,
        MEMORY_LIMIT_EXCEEDED,
        SYSTEM_ERROR
    }

    private final Verdict verdict;
    private final String output;
    private final String error;
    private final long executionTimeMs;

    public ExecutionResult(
            Verdict verdict,
            String output,
            String error,
            long executionTimeMs) {

        this.verdict = verdict;
        this.output = output;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}