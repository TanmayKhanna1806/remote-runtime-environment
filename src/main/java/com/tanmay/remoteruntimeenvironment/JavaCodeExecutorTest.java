package com.tanmay.remoteruntimeenvironment.executor;

public class JavaCodeExecutorTest {

    public static void main(String[] args) {

        JavaCodeExecutor executor = new JavaCodeExecutor();

        String sourceCode = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
                """;

        ExecutionResult result =
                executor.execute(sourceCode, "Hello World");

        System.out.println("Verdict: " + result.getVerdict());
        System.out.println("Output: " + result.getOutput());
        System.out.println("Error: " + result.getError());
        System.out.println("Execution time: "
                + result.getExecutionTimeMs() + " ms");
    }
}