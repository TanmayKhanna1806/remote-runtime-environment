package com.tanmay.remoteruntimeenvironment.executor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class JavaCodeExecutor {

    private static final long COMPILATION_TIMEOUT_SECONDS = 10;
    private static final long EXECUTION_TIMEOUT_SECONDS = 5;
    private static final long MAX_OUTPUT_BYTES = 1024 * 1024; // 1 MB
    private static final long MAX_MEMORY_MB = 256;

    public ExecutionResult execute(String sourceCode, String expectedOutput) {

        Path workingDirectory = null;

        try {
            workingDirectory = Files.createTempDirectory("submission-");

            Path sourceFile = workingDirectory.resolve("Solution.java");
            Files.writeString(sourceFile, sourceCode);

            // Compile
            Process compileProcess = new ProcessBuilder(
                    "javac",
                    "Solution.java"
            )
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean compilationFinished = compileProcess.waitFor(
                    COMPILATION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
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

            String compilerOutput =
                    new String(compileProcess.getInputStream().readAllBytes());

            if (compileProcess.exitValue() != 0) {
                return new ExecutionResult(
                        ExecutionResult.Verdict.COMPILE_ERROR,
                        "",
                        compilerOutput,
                        0
                );
            }

            // Execute
            long startTime = System.currentTimeMillis();

            Process executionProcess = new ProcessBuilder(
                    "java",
                    "-Xmx" + MAX_MEMORY_MB + "m",
                    "Solution"
            )
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            ByteArrayOutputStream outputBuffer =
                    new ByteArrayOutputStream();

            Thread outputReader = new Thread(() ->
                    readOutput(
                            executionProcess,
                            outputBuffer
                    )
            );

            outputReader.start();

            boolean executionFinished = executionProcess.waitFor(
                    EXECUTION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            long executionTime =
                    System.currentTimeMillis() - startTime;

            // Check whether output limit was exceeded
            if (outputBuffer.size() > MAX_OUTPUT_BYTES) {

                executionProcess.destroyForcibly();
                outputReader.join(1000);

                return new ExecutionResult(
                        ExecutionResult.Verdict.SYSTEM_ERROR,
                        outputBuffer.toString(),
                        "Output exceeded the 1 MB limit.",
                        executionTime
                );
            }

            // Time limit
            if (!executionFinished) {

                executionProcess.destroyForcibly();
                outputReader.join(1000);

                return new ExecutionResult(
                        ExecutionResult.Verdict.TIME_LIMIT_EXCEEDED,
                        outputBuffer.toString(),
                        "Execution exceeded the time limit.",
                        executionTime
                );
            }

            outputReader.join(1000);

            String output = outputBuffer.toString();

            // Runtime error
            if (executionProcess.exitValue() != 0) {

                return new ExecutionResult(
                        ExecutionResult.Verdict.RUNTIME_ERROR,
                        output,
                        "Program exited with code "
                                + executionProcess.exitValue(),
                        executionTime
                );
            }

            // Output comparison
            String actual = normalizeOutput(output);
            String expected = normalizeOutput(expectedOutput);

            if (actual.equals(expected)) {

                return new ExecutionResult(
                        ExecutionResult.Verdict.ACCEPTED,
                        output,
                        "",
                        executionTime
                );
            }

            return new ExecutionResult(
                    ExecutionResult.Verdict.WRONG_ANSWER,
                    output,
                    "Output does not match expected output.",
                    executionTime
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
            ByteArrayOutputStream outputBuffer
    ) {

        try (InputStream inputStream = process.getInputStream()) {

            byte[] buffer = new byte[8192];

            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {

                outputBuffer.write(buffer, 0, bytesRead);

                if (outputBuffer.size() > MAX_OUTPUT_BYTES) {
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