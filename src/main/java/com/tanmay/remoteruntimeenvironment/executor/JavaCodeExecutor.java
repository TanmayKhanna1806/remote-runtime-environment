package com.tanmay.remoteruntimeenvironment.executor;

import com.tanmay.remoteruntimeenvironment.model.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JavaCodeExecutor {

    private static final long EXECUTION_TIMEOUT_SECONDS = 5;
    private static final long COMPILATION_TIMEOUT_SECONDS = 10;
    private static final long MAX_OUTPUT_BYTES = 1024 * 1024;
    private static final long MAX_MEMORY_MB = 256;

    private static final String DOCKER_IMAGE =
            "remote-runtime-java:17";

    public ExecutionResult execute(
            String sourceCode,
            List<TestCase> testCases) {

        Path workingDirectory = null;

        try {
            workingDirectory =
                    Files.createTempDirectory("submission-");

            Path sourceFile =
                    workingDirectory.resolve("Solution.java");

            Files.writeString(sourceFile, sourceCode);

            // Compile once inside Docker.
            Process compileProcess = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "--network", "none",
                    "--memory", MAX_MEMORY_MB + "m",
                    "--cpus", "1",
                    "--pids-limit", "64",
                    "--read-only",
                    "--tmpfs",
                    "/sandbox:rw,nosuid,size=64m,uid=1001,gid=1001",
                    "--mount",
                    "type=bind,source="
                            + sourceFile.toAbsolutePath()
                            + ",target=/input/Solution.java,readonly",
                    DOCKER_IMAGE,
                    "sh", "-c",
                    "cp /input/Solution.java /sandbox/Solution.java && "
                            + "javac /sandbox/Solution.java"
            )
                    .redirectErrorStream(true)
                    .start();

            boolean compilationFinished =
                    compileProcess.waitFor(
                            COMPILATION_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            String compilerOutput =
                    new String(
                            compileProcess
                                    .getInputStream()
                                    .readAllBytes(),
                            StandardCharsets.UTF_8
                    );

            if (!compilationFinished) {

                compileProcess.destroyForcibly();

                return new ExecutionResult(
                        ExecutionResult.Verdict.SYSTEM_ERROR,
                        "",
                        "Compilation timed out.",
                        0
                );
            }

            if (compileProcess.exitValue() != 0) {

                return new ExecutionResult(
                        ExecutionResult.Verdict.COMPILE_ERROR,
                        "",
                        compilerOutput,
                        0
                );
            }

            /*
             * The compilation container is removed after it exits,
             * so the compiled class must be produced again for execution.
             *
             * Each test case therefore gets its own isolated container.
             */
            long totalExecutionTime = 0;

            for (int i = 0; i < testCases.size(); i++) {

                TestCase testCase = testCases.get(i);

                long startTime =
                        System.currentTimeMillis();

                /*
                 * Create a temporary input file on the host.
                 * This avoids stdin being consumed by the shell
                 * before it reaches the Java program.
                 */
                Path inputFile =
                        workingDirectory.resolve(
                                "input-" + i + ".txt"
                        );

                Files.writeString(
                        inputFile,
                        testCase.getInput() == null
                                ? ""
                                : testCase.getInput()
                );

                Process process = new ProcessBuilder(
                        "docker", "run", "--rm",
                        "--network", "none",
                        "--memory", MAX_MEMORY_MB + "m",
                        "--cpus", "1",
                        "--pids-limit", "64",
                        "--read-only",
                        "--tmpfs",
                        "/sandbox:rw,nosuid,size=64m,uid=1001,gid=1001",
                        "--mount",
                        "type=bind,source="
                                + sourceFile.toAbsolutePath()
                                + ",target=/input/Solution.java,readonly",
                        "--mount",
                        "type=bind,source="
                                + inputFile.toAbsolutePath()
                                + ",target=/input/test.txt,readonly",
                        DOCKER_IMAGE,
                        "sh", "-c",
                        "cp /input/Solution.java /sandbox/Solution.java && "
                                + "javac /sandbox/Solution.java && "
                                + "java -Xmx"
                                + MAX_MEMORY_MB
                                + "m -cp /sandbox Solution "
                                + "< /input/test.txt"
                )
                        .redirectErrorStream(true)
                        .start();

                ByteArrayOutputStream outputBuffer =
                        new ByteArrayOutputStream();

                Thread outputReader =
                        new Thread(() ->
                                readOutput(
                                        process,
                                        outputBuffer
                                )
                        );

                outputReader.start();

                boolean finished =
                        process.waitFor(
                                EXECUTION_TIMEOUT_SECONDS,
                                TimeUnit.SECONDS
                        );

                long executionTime =
                        System.currentTimeMillis()
                                - startTime;

                totalExecutionTime += executionTime;

                if (outputBuffer.size()
                        > MAX_OUTPUT_BYTES) {

                    process.destroyForcibly();

                    outputReader.join(1000);

                    return new ExecutionResult(
                            ExecutionResult.Verdict.SYSTEM_ERROR,
                            outputBuffer.toString(),
                            "Output exceeded the 1 MB limit on test case "
                                    + (i + 1),
                            totalExecutionTime
                    );
                }

                if (!finished) {

                    process.destroyForcibly();

                    outputReader.join(1000);

                    return new ExecutionResult(
                            ExecutionResult.Verdict.TIME_LIMIT_EXCEEDED,
                            outputBuffer.toString(),
                            "Execution exceeded the time limit on test case "
                                    + (i + 1),
                            totalExecutionTime
                    );
                }

                outputReader.join(1000);

                String output =
                        outputBuffer.toString();

                if (process.exitValue() != 0) {

                    return new ExecutionResult(
                            ExecutionResult.Verdict.RUNTIME_ERROR,
                            output,
                            "Runtime error on test case "
                                    + (i + 1)
                                    + ". Exit code: "
                                    + process.exitValue(),
                            totalExecutionTime
                    );
                }

                String actual =
                        normalizeOutput(output);

                String expected =
                        normalizeOutput(
                                testCase.getExpectedOutput()
                        );

                if (!actual.equals(expected)) {

                    return new ExecutionResult(
                            ExecutionResult.Verdict.WRONG_ANSWER,
                            output,
                            "Wrong answer on test case "
                                    + (i + 1),
                            totalExecutionTime
                    );
                }
            }

            return new ExecutionResult(
                    ExecutionResult.Verdict.ACCEPTED,
                    "All " + testCases.size()
                            + " test cases passed.",
                    "",
                    totalExecutionTime
            );

        } catch (IOException e) {

            return new ExecutionResult(
                    ExecutionResult.Verdict.SYSTEM_ERROR,
                    "",
                    e.getMessage(),
                    0
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return new ExecutionResult(
                    ExecutionResult.Verdict.SYSTEM_ERROR,
                    "",
                    "Execution was interrupted.",
                    0
            );

        } finally {

            if (workingDirectory != null) {
                deleteDirectory(workingDirectory);
            }
        }
    }

    private void readOutput(
            Process process,
            ByteArrayOutputStream outputBuffer) {

        try (InputStream inputStream =
                     process.getInputStream()) {

            byte[] buffer = new byte[8192];

            int bytesRead;

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                outputBuffer.write(
                        buffer,
                        0,
                        bytesRead
                );

                if (outputBuffer.size()
                        > MAX_OUTPUT_BYTES) {

                    process.destroyForcibly();
                    break;
                }
            }

        } catch (IOException ignored) {
        }
    }

    private String normalizeOutput(String output) {

        if (output == null) {
            return "";
        }

        return output
                .replace("\r\n", "\n")
                .trim();
    }

    private void deleteDirectory(Path directory) {

        try {

            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {

                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }

                    });

        } catch (IOException ignored) {
        }
    }
}