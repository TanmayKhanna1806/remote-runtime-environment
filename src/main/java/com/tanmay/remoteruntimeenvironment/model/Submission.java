package com.tanmay.remoteruntimeenvironment.model;

import com.tanmay.remoteruntimeenvironment.executor.ExecutionResult;

import java.util.List;

public class Submission {

    private final Long id;
    private final String sourceCode;
    private final List<TestCase> testCases;

    private ExecutionResult.Verdict verdict;
    private String output;
    private String error;
    private long executionTimeMs;

    public Submission(
            Long id,
            String sourceCode,
            List<TestCase> testCases) {

        this.id = id;
        this.sourceCode = sourceCode;
        this.testCases = testCases;
        this.verdict = null;
        this.output = "";
        this.error = "";
        this.executionTimeMs = 0;
    }

    public Long getId() {
        return id;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public ExecutionResult.Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(ExecutionResult.Verdict verdict) {
        this.verdict = verdict;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}