package com.tanmay.remoteruntimeenvironment.executor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class JavaCodeExecutor {

    private static final long COMPILATION_TIMEOUT_SECONDS = 10;

    public ExecutionResult execute(String sourceCode, String expectedOutput) {

        Path workingDirectory = null;

        try {
            // Create an isolated temporary directory for this submission
            workingDirectory = Files.createTempDirectory("submission-");

            // Java requires the public class name to match the file name
            Path sourceFile = workingDirectory.resolve("Solution.java");

            Files.writeString(sourceFile, sourceCode);

            // Compile the submitted source code
            Process compileProcess = new ProcessBuilder(
                    "javac",
                    sourceFile.getFileName().toString()
            )
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean finished = compileProcess.waitFor(
                    COMPILATION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!finished) {
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

            // Compilation succeeded.
            // Execution will be implemented in the next stage.
            return new ExecutionResult(
                    ExecutionResult.Verdict.SYSTEM_ERROR,
                    "",
                    "Compilation successful. Execution not implemented yet.",
                    0
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
                    "Compilation process was interrupted.",
                    0
            );
        }
    }
}