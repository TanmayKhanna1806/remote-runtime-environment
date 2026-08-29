# Remote Runtime Environment

A remote code execution and judging system designed for competitive programming events.

The system accepts source code submissions, compiles and executes them against predefined test cases, applies execution and resource limits, and returns a verdict based on correctness and efficiency.

## Problem Statement

The goal of this project is to develop a Remote Runtime Environment for a speed-coding event.

Participants submit code which must be:

1. Received by the backend
2. Compiled
3. Executed in a controlled environment
4. Tested against predefined inputs
5. Evaluated for correctness
6. Monitored for resource and execution limits
7. Assigned a final verdict

The system must also handle abnormal submissions such as infinite loops, excessive memory usage, runtime errors, compilation failures, and excessive output.

## Goals

The main goals of the system are:

- Reliable code compilation and execution
- Deterministic judging
- Execution time limits
- Memory and resource protection
- Correct handling of failed submissions
- Concurrent submission processing
- Scalable backend architecture
- Low-cost deployment
- Reproducible development and deployment

## Architecture

```text
                    Participant
                         |
                         v
                  +---------------+
                  |   REST API    |
                  +-------+-------+
                          |
                          v
                  +---------------+
                  |  Submission   |
                  |    Service    |
                  +-------+-------+
                          |
                          v
                  +---------------+
                  |  Worker Pool  |
                  +-------+-------+
                          |
                          v
                  +---------------+
                  |    Executor   |
                  +-------+-------+
                          |
                          v
                  +---------------+
                  | Docker Runtime|
                  |    Java 17    |
                  +-------+-------+
                          |
                          v
                  +---------------+
                  |     Judge     |
                  +-------+-------+
                          |
                          v
                        Result
```

## Execution Pipeline

```text
Code Submission
       |
       v
Validation
       |
       v
Compilation
       |
       +---- Compilation Failure ---> COMPILE_ERROR
       |
       v
Execution
       |
       +---- Timeout -------------> TIME_LIMIT_EXCEEDED
       |
       +---- Resource Failure ----> SYSTEM_ERROR
       |
       +---- Runtime Failure -----> RUNTIME_ERROR
       |
       v
Output Comparison
       |
       +---- Incorrect -----------> WRONG_ANSWER
       |
       v
ACCEPTED
```

## Verdicts

The system supports the following verdicts:

- `ACCEPTED`
- `WRONG_ANSWER`
- `COMPILE_ERROR`
- `RUNTIME_ERROR`
- `TIME_LIMIT_EXCEEDED`
- `SYSTEM_ERROR`

## Technology Stack

- Java 17
- Spring Boot
- Gradle
- Docker
- Java ExecutorService
- ConcurrentHashMap
- JUnit

The current implementation focuses on Java submissions. Multi-language support can be added later using separate runtime images.

## Project Structure

```text
remote-runtime-environment/
|
+-- src/
|   +-- main/
|   |   +-- java/
|   |       +-- com/tanmay/remoteruntimeenvironment/
|   |           +-- controller/
|   |           +-- dto/
|   |           +-- executor/
|   |           +-- model/
|   |           +-- service/
|   |
|   +-- test/
|
+-- Dockerfile
+-- build.gradle
+-- gradlew
+-- gradlew.bat
+-- README.md
+-- .gitignore
```

## Execution Model

Submitted source code is treated as untrusted input.

The execution layer is responsible for:

1. Creating a temporary working environment for a submission
2. Compiling the submitted source code
3. Capturing compilation errors
4. Executing the compiled program inside a Docker container
5. Applying execution and resource limits
6. Capturing standard output and errors
7. Comparing output against expected output
8. Returning the appropriate verdict
9. Cleaning up temporary execution resources

The current implementation uses Docker-based isolated execution for submitted Java programs.

## Docker Isolation

Submitted code is not executed directly on the host machine.

Each execution runs inside a Docker container with resource and isolation restrictions.

### Network Isolation

```text
--network none
```

The submitted program has no network access during execution.

### Memory Limit

```text
--memory 256m
```

The container is limited to 256 MB of memory.

The Java process also uses:

```text
-Xmx256m
```

to limit the Java heap.

### CPU Limit

```text
--cpus 1
```

Each execution is restricted to one CPU.

### Process Limit

```text
--pids-limit 64
```

This limits the number of processes that can be created inside the container.

### Read-only Filesystem

```text
--read-only
```

The container filesystem is read-only by default.

### Temporary Writable Sandbox

```text
--tmpfs /sandbox:rw,nosuid,size=64m
```

The sandbox provides temporary writable storage required for compilation and execution.

### Read-only Source Mount

The submitted source file is mounted into the container as read-only before being copied into the temporary sandbox.

These restrictions provide multiple layers of protection against resource abuse and unwanted interaction with the host environment.

## Execution Timeout

Each test case has a maximum execution time of 5 seconds.

If execution does not finish within this limit, it is forcibly terminated and the submission receives:

```text
TIME_LIMIT_EXCEEDED
```

For example:

```java
public class Solution {
    public static void main(String[] args) {
        while (true) {
        }
    }
}
```

cannot run indefinitely.

## Output Limit

The executor limits captured output to 1 MB.

This protects the backend from programs that continuously generate output.

For example:

```java
public class Solution {
    public static void main(String[] args) {
        while (true) {
            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAA");
        }
    }
}
```

will eventually exceed the configured output limit.

The process is terminated and the execution is reported as a system/output-limit failure.

## Output Normalization

Before comparing outputs, the executor:

1. Converts Windows line endings (`\r\n`) to Unix line endings (`\n`)
2. Trims leading and trailing whitespace

This prevents harmless line-ending or surrounding-whitespace differences from incorrectly producing a wrong answer.

## Concurrent Execution

The backend uses a fixed-size `ExecutorService` worker pool containing four workers.

```text
                    REST API
                       |
          +------------+------------+
          |            |            |
          v            v            v
       Worker 1     Worker 2     Worker 3
          |            |            |
          v            v            v
       Docker        Docker        Docker
       Runtime       Runtime       Runtime
```

This prevents every incoming submission from creating unlimited concurrent execution processes.

It also separates code execution from HTTP request handling.

For larger deployments, the local worker pool can be replaced by a distributed queue such as Redis, RabbitMQ, or a cloud-managed queue. Execution workers can then be scaled independently from API servers.

## Submission Lifecycle

```text
POST Submission
       |
       v
Create Submission ID
       |
       v
Store Submission
       |
       v
Submit Execution to Worker Pool
       |
       v
Run Test Cases
       |
       v
Generate ExecutionResult
       |
       v
Update Submission
       |
       v
Retrieve Submission Result
```

The asynchronous execution model prevents long-running submissions from blocking the API request-processing thread.

## Determinism

The judging process is designed to produce consistent results for the same submission and test case.

Determinism is supported through:

- Fixed test cases
- Controlled execution environment
- Fixed Java 17 runtime
- No external network access
- Consistent resource limits
- Deterministic output comparison
- Fresh execution environments for test cases

```text
Same Submission
       +
Same Input
       +
Same Runtime Environment
       +
Same Resource Limits
       =
Same Expected Verdict
```

## Security and Isolation

Submitted code is considered untrusted.

The execution environment therefore applies several independent restrictions:

```text
No Network
    +
CPU Limit
    +
Memory Limit
    +
PID Limit
    +
Execution Timeout
    +
Output Limit
    +
Read-Only Filesystem
    +
Temporary Sandbox
```

The goal is to prevent a participant program from indefinitely consuming resources or accessing services that are not required for solving the programming problem.

The current project focuses on the core runtime and judging system. Authentication and role-based access control are not part of the current implementation.

For a production event platform, authentication and authorization would be added at the API layer.

## Handling Abnormal Submissions

The system is designed to handle common abnormal execution scenarios.

### Infinite Loops

A submission such as:

```
java
while (true) {
}
```

must not be allowed to run indefinitely.

The execution process is given a maximum execution time. If the limit is exceeded, the process is terminated and the submission receives:

```text
TIME_LIMIT_EXCEEDED
```

### Compilation Errors

If the submitted source code cannot be compiled, compilation output is captured and the submission receives:

```text
COMPILE_ERROR
```

### Runtime Errors

If compilation succeeds but the program terminates abnormally during execution, the submission receives:

```text
RUNTIME_ERROR
```

### Excessive Memory Usage

The execution environment applies memory and resource restrictions so that a submission cannot consume unlimited system resources.

### Excessive Output

A submission producing extremely large amounts of output can consume significant system resources.

The execution layer therefore applies an output limit and terminates execution if the configured limit is exceeded.

### Concurrent Submissions

A large number of submissions could otherwise create uncontrolled processes.

The fixed-size worker pool limits the number of concurrent executions.

### Temporary Files

Compilation generates temporary source and class files.

Each submission receives a temporary working directory, which is cleaned up after execution.

## Testing

The execution engine has been tested against the following scenarios:

| Scenario | Expected Result |
|---|---|
| Correct program | `ACCEPTED` |
| Incorrect output | `WRONG_ANSWER` |
| Invalid Java source | `COMPILE_ERROR` |
| Runtime failure | `RUNTIME_ERROR` |
| Infinite loop | `TIME_LIMIT_EXCEEDED` |
| Excessive output | `SYSTEM_ERROR` |

The project also includes automated Gradle/JUnit tests.

Run:

```powershell
.\gradlew.bat test
```

Build the complete project with:

```powershell
.\gradlew.bat clean build
```

## API Design

The backend exposes REST endpoints for interacting with the runtime environment.

The core submission workflow includes:

```http
POST /api/submissions
```

A submission contains the source code and test cases required for execution.

A submission is assigned an identifier that can be used to retrieve its execution result.

For example:

```http
GET /api/submissions/{id}
```

Possible execution states include:

```text
PENDING
RUNNING
ACCEPTED
WRONG_ANSWER
COMPILE_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
SYSTEM_ERROR
```

## Running Locally

### Prerequisites

Install:

- JDK 17
- Docker Desktop
- Git

### Clone the Repository

```bash
git clone <YOUR_PUBLIC_GITHUB_REPOSITORY>
cd remote-runtime-environment
```

### Build the Application

Windows:

```powershell
.\gradlew.bat clean build
```

### Build the Runtime Image

```bash
docker build -t remote-runtime-java:17 .
```

### Verify Docker

```bash
docker run --rm hello-world
```

### Run the Application

```powershell
.\gradlew.bat bootRun
```

The Spring Boot application will start using the configured application port.

## Scalability

The current implementation uses a fixed-size local worker pool.

This is intentionally lightweight and suitable for a prototype or small event.

For a larger competition, the architecture can evolve into:

```text
                         Load Balancer
                              |
                              v
                    +-------------------+
                    |    API Servers    |
                    +---------+---------+
                              |
                              v
                       Message Queue
                    /        |        \
                   v         v         v
               Worker 1  Worker 2  Worker 3
                   |         |         |
                   v         v         v
               Docker    Docker    Docker
               Runtime   Runtime   Runtime
```

This architecture allows:

- API servers to scale independently
- Execution workers to scale independently
- Queue-based workload management
- Backpressure during traffic spikes
- Better fault isolation
- Worker autoscaling

## Cost Efficiency

The system is designed to avoid uncontrolled resource consumption.

Per-execution controls include:

- CPU limit
- Memory limit
- Process limit
- Execution timeout
- Output limit
- Fixed worker concurrency
- Temporary execution environments
- Cleanup after execution

For a larger cloud deployment, worker instances can be scaled according to queue depth instead of running a large number of workers continuously.

The system also avoids storing unnecessary temporary execution artifacts persistently.

## Persistence

The current implementation keeps active submissions in memory using `ConcurrentHashMap` and generates submission IDs using `AtomicLong`.

This keeps the prototype lightweight and avoids adding unnecessary infrastructure to the core runtime.

For a production deployment, PostgreSQL or another persistent database could store:

- Problems
- Test cases
- Submissions
- Verdicts
- Execution statistics
- Participant submission history

Temporary compilation files and execution artifacts should remain ephemeral rather than being stored unnecessarily in the database.

## Development Approach

The project prioritizes a reliable working core over unnecessary infrastructure complexity.

The current implementation focuses on:

1. Code submission
2. Compilation
3. Docker-based execution
4. Timeout handling
5. Output comparison
6. Verdict generation
7. Concurrent execution
8. Resource isolation
9. Reproducible deployment

More advanced infrastructure is treated as an extension rather than adding unnecessary complexity to the core system.

## Engineering Trade-offs

The project prioritizes a reliable working core while maintaining a clear path toward a production architecture.

The current implementation uses a fixed-size worker pool and Docker-based execution rather than immediately introducing a distributed job queue and multiple execution servers.

This keeps the system lightweight, easier to reproduce, and suitable for a small competition while still allowing the execution layer to be scaled independently in a larger deployment.

For production use, the worker pool could be replaced with a distributed queue and horizontally scalable execution workers.

## Known Limitations

The current implementation is a focused Remote Runtime Environment rather than a complete competitive programming platform.

Current limitations include:

- Java is the primary supported language
- Submission state is currently stored in memory
- Worker coordination is local to the application instance
- Authentication and RBAC are not implemented
- Advanced monitoring and observability are not implemented
- Distributed job queues are not implemented
- Production-scale multi-node orchestration is not implemented

These limitations are intentional scope decisions for the current implementation.

## Reproducibility

The project is designed to be reproducible using:

- Java 17
- Spring Boot
- Gradle
- Docker

The participant execution environment is packaged as:

```text
remote-runtime-java:17
```

The Docker runtime image provides a consistent Java 17 compiler/runtime environment for submitted programs rather than relying on the host machine's Java installation.

A developer can clone the repository, build the Spring Boot application, build the Docker runtime image, and run the project using the documented commands.

## Future Improvements

Possible future improvements include:

- Multi-language support
- Separate Docker runtime images for each language
- PostgreSQL persistence
- Redis/RabbitMQ/cloud-managed job queues
- Horizontally scalable execution workers
- Worker autoscaling
- Authentication and role-based access control
- API rate limiting
- Detailed CPU and memory usage measurement
- Monitoring and observability
- Submission history and analytics
- Stronger sandbox hardening
- Improved API validation

## Testing Strategy

The execution engine is tested against both valid and abnormal submissions.

Important scenarios include:

### Correct Submission

Expected:

```text
ACCEPTED
```

### Wrong Output

Expected:

```text
WRONG_ANSWER
```

### Compilation Failure

Expected:

```text
COMPILE_ERROR
```

### Runtime Failure

Expected:

```text
RUNTIME_ERROR
```

### Infinite Loop

Expected:

```text
TIME_LIMIT_EXCEEDED
```

### Excessive Output

Expected:

```text
SYSTEM_ERROR
```

These tests verify that the execution environment handles both normal program behaviour and resource-abuse scenarios.

## Project Status

The current implementation provides a working core Remote Runtime Environment capable of:

- Receiving code submissions
- Compiling Java 17 source code
- Executing code inside Docker
- Running predefined test cases
- Comparing program output
- Enforcing CPU, memory, PID, timeout, filesystem, network, and output restrictions
- Handling abnormal submissions
- Processing submissions asynchronously
- Returning execution verdicts

The project prioritizes a well-designed, reproducible, and working core over unnecessary feature completeness, while maintaining a clear architecture for future scaling and additional languages.
```

**Now this will give you ONE Copy button.** When pasted into `README.md`, the `##` lines will render as actual headings, while the inner triple-backtick sections will render as code blocks.