package com.tanmay.remoteruntimeenvironment;

import com.tanmay.remoteruntimeenvironment.executor.ExecutionResult;
import com.tanmay.remoteruntimeenvironment.executor.JavaCodeExecutor;

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
                executor.execute(acceptedCode, "Hello World");

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
                executor.execute(wrongAnswerCode, "Hello World");

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
                executor.execute(compileErrorCode, "Hello");

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
                executor.execute(infiniteLoopCode, "");

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
                executor.execute(hugeOutputCode, "");

        System.out.println("TEST 5: " + outputLimit.getVerdict());
        System.out.println("TEST 5 ERROR: " + outputLimit.getError());
    }
}