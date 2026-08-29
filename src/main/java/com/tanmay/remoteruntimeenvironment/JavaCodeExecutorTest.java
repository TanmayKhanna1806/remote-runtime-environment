package com.tanmay.remoteruntimeenvironment;

import com.tanmay.remoteruntimeenvironment.executor.ExecutionResult;
import com.tanmay.remoteruntimeenvironment.executor.JavaCodeExecutor;
import com.tanmay.remoteruntimeenvironment.model.TestCase;

import java.util.List;

public class JavaCodeExecutorTest {

    public static void main(String[] args) {

        JavaCodeExecutor executor = new JavaCodeExecutor();

        // TEST 1: Accepted
        String acceptedCode = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
                """;

        ExecutionResult accepted =
                executor.execute(
                        acceptedCode,
                        List.of(new TestCase("", "Hello World"))
                );

        System.out.println("TEST 1: " + accepted.getVerdict());


        // TEST 2: Wrong Answer
        String wrongAnswerCode = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println("Wrong");
                    }
                }
                """;

        ExecutionResult wrongAnswer =
                executor.execute(
                        wrongAnswerCode,
                        List.of(new TestCase("", "Hello World"))
                );

        System.out.println("TEST 2: " + wrongAnswer.getVerdict());


        // TEST 3: Compile Error
        String compileErrorCode = """
                public class Solution {
                    public static void main(String[] args) {
                        System.out.println("Hello"
                    }
                }
                """;

        ExecutionResult compileError =
                executor.execute(
                        compileErrorCode,
                        List.of(new TestCase("", "Hello"))
                );

        System.out.println("TEST 3: " + compileError.getVerdict());


        // TEST 4: Time Limit Exceeded
        String infiniteLoopCode = """
                public class Solution {
                    public static void main(String[] args) {
                        while (true) {
                        }
                    }
                }
                """;

        ExecutionResult timeout =
                executor.execute(
                        infiniteLoopCode,
                        List.of(new TestCase("", ""))
                );

        System.out.println("TEST 4: " + timeout.getVerdict());


        // TEST 5: Output Limit
        String hugeOutputCode = """
                public class Solution {
                    public static void main(String[] args) {
                        while (true) {
                            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        }
                    }
                }
                """;

        ExecutionResult outputLimit =
                executor.execute(
                        hugeOutputCode,
                        List.of(new TestCase("", ""))
                );

        System.out.println("TEST 5: " + outputLimit.getVerdict());
        System.out.println("TEST 5 ERROR: " + outputLimit.getError());


        // TEST 6: Multiple Test Cases
        String multiTestCode = """
                import java.util.Scanner;

                public class Solution {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        int n = scanner.nextInt();
                        System.out.println(n * n);
                    }
                }
                """;

        ExecutionResult multiple =
                executor.execute(
                        multiTestCode,
                        List.of(
                                new TestCase("5\n", "25"),
                                new TestCase("10\n", "100"),
                                new TestCase("7\n", "49")
                        )
                );

        System.out.println("TEST 6: " + multiple.getVerdict());
        System.out.println("TEST 6 OUTPUT: " + multiple.getOutput());
    }
}