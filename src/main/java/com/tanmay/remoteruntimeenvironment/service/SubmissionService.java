package com.tanmay.remoteruntimeenvironment.service;

import com.tanmay.remoteruntimeenvironment.executor.ExecutionResult;
import com.tanmay.remoteruntimeenvironment.executor.JavaCodeExecutor;
import com.tanmay.remoteruntimeenvironment.model.Submission;
import com.tanmay.remoteruntimeenvironment.model.TestCase;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SubmissionService {

    private final JavaCodeExecutor executor = new JavaCodeExecutor();

    private final AtomicLong idGenerator = new AtomicLong(1);

    private final Map<Long, Submission> submissions =
            new ConcurrentHashMap<>();

    private final ExecutorService workerPool =
            Executors.newFixedThreadPool(4);

    public Submission submit(
            String sourceCode,
            List<TestCase> testCases) {

        Long id = idGenerator.getAndIncrement();

        Submission submission =
                new Submission(id, sourceCode, testCases);

        submissions.put(id, submission);

        workerPool.submit(() -> {

            ExecutionResult result =
                    executor.execute(sourceCode, testCases);

            submission.setVerdict(result.getVerdict());
            submission.setOutput(result.getOutput());
            submission.setError(result.getError());
            submission.setExecutionTimeMs(
                    result.getExecutionTimeMs()
            );
        });

        return submission;
    }

    public Submission getSubmission(Long id) {
        return submissions.get(id);
    }
}