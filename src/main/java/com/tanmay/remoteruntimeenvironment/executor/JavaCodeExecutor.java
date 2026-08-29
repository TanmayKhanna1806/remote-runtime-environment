package com.tanmay.remoteruntimeenvironment.executor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class JavaCodeExecutor {

    private static final long COMPILATION_TIMEOUT_SECONDS = 10;
    private static final long EXECUTION_TIMEOUT_SECONDS = 5;

    public ExecutionResult execute(String sourceCode, String expectedOutput) {

        Path workingDirectory = null;

        try {
            // 1. Create a temporary directory for this submission
            workingDirectory = Files.createTempDirectory("submission-");

            // 2. Write submitted source code
            Path sourceFile = workingDirectory.resolve("Solution.java");
            Files.writeString(sourceFile, sourceCode);

            // 3. Compile
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

            // 4. Execute compiled program
            long startTime = System.currentTimeMillis();

            Process executionProcess = new ProcessBuilder(
                    "java",
                    "Solution"
            )
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean executionFinished = executionProcess.waitFor(
                    EXECUTION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            long executionTime =
                    System.currentTimeMillis() - startTime;

            // 5. Handle timeout
            if (!executionFinished) {

                executionProcess.destroyForcibly();

                return new ExecutionResult(
                        ExecutionResult.Verdict.TIME_LIMIT_EXCEEDED,
                        "",
                        "Execution exceeded the time limit.",
                        executionTime
                );
            }

            // 6. Capture program output
            String output =
                    new String(executionProcess.getInputStream().readAllBytes());

            // 7. Handle runtime error
            if (executionProcess.exitValue() != 0) {

                return new ExecutionResult(
                        ExecutionResult.Verdict.RUNTIME_ERROR,
                        output,
                        "Program exited with code "
                                + executionProcess.exitValue(),
                        executionTime
                );
            }

            // 8. Compare output
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

            // 9. Delete temporary submission directory
            if (workingDirectory != null) {
                deleteDirectory(workingDirectory);
            }
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