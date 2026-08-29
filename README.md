# Remote Runtime Environment

A remote code execution and judging system designed for competitive programming events.

The system accepts source code submissions, compiles and executes them against predefined test cases, applies execution and resource limits, and returns a verdict based on correctness and efficiency.

---

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

The system must also handle abnormal submissions such as infinite loops, excessive memory usage, runtime errors and compilation failures.

---

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

---

## Planned Architecture

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
                 |  Controlled   |
                 |    Runtime    |
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

---

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
       +---- Resource Failure ----> MEMORY_LIMIT_EXCEEDED
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

---

## Verdicts

The system will support the following verdicts:

- `ACCEPTED`
- `WRONG_ANSWER`
- `COMPILE_ERROR`
- `RUNTIME_ERROR`
- `TIME_LIMIT_EXCEEDED`
- `MEMORY_LIMIT_EXCEEDED`
- `SYSTEM_ERROR`

---

## Technology Stack

- Java
- Spring Boot
- Maven
- H2 / PostgreSQL
- Java ExecutorService
- Linux process utilities
- Docker for future stronger isolation

---

## Project Structure

```text
remote-runtime-environment/
│
├── src/
│   └── main/
│       └── java/
│
├── docker/
│
├── README.md
├── pom.xml
└── .gitignore
```

The project structure will evolve as more components are implemented.

---

## Execution Model

Submitted source code is treated as untrusted input.

The execution layer is responsible for:

1. Creating a temporary working environment for a submission
2. Compiling the submitted source code
3. Capturing compilation errors
4. Executing the compiled program
5. Applying execution and resource limits
6. Capturing standard output and errors
7. Comparing the output against the expected output
8. Returning the appropriate verdict
9. Cleaning up temporary execution resources

The initial implementation uses a lightweight process-based execution model.

---

## Security and Isolation

Submitted code is considered untrusted.

The execution layer therefore applies restrictions such as:

- Maximum execution time
- Memory limits
- Process limits
- Output limits
- Restricted execution environment
- No unnecessary network access

The initial implementation uses operating-system-level process controls for the execution layer.

A stronger production deployment would use isolated containers or another dedicated sandboxing mechanism for each execution.

---

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

The execution process is given a maximum execution time. If this limit is exceeded, the process is terminated and the submission receives:

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

If the configured memory limit is exceeded, the submission can be classified as:

```text
MEMORY_LIMIT_EXCEEDED
```

### Excessive Output

A submission producing extremely large amounts of output can consume significant system resources.

The execution layer therefore applies an output limit and terminates execution if the configured limit is exceeded.

---

## Determinism

The judging process should produce consistent results for the same submission and test case.

Determinism is supported through:

- Fixed test cases
- Controlled execution environment
- Fixed runtime/compiler version
- No dependency on external network services
- Consistent resource limits
- Deterministic output comparison

The intended execution model is:

```text
Same Submission
       +
Same Input
       +
Same Runtime Environment
       =
Same Expected Verdict
```

---

## Scalability

The initial implementation uses a fixed-size worker pool so that incoming API requests do not directly execute submitted programs on the request-handling thread.

Conceptually:

```text
                    REST API
                       |
                       v
                  Worker Pool
                 /     |     \
                v      v      v
             Worker  Worker  Worker
                |      |      |
                v      v      v
             Runtime Runtime Runtime
```

A fixed-size worker pool limits the number of concurrent executions and prevents an uncontrolled number of submissions from consuming all available system resources.

For a production-scale deployment, the in-process worker pool can be replaced with a distributed queue such as Redis, RabbitMQ or a cloud-managed queue.

Execution workers can then be scaled independently from the API servers.

---

## Cost Efficiency

The system limits resource consumption per submission to prevent a small number of submissions from exhausting the available infrastructure.

Potential production optimizations include:

- Worker autoscaling
- Resource limits per execution
- Ephemeral execution environments
- Queue-based workload distribution
- Limiting source code size
- Limiting output size
- Avoiding unnecessary persistent storage
- Scaling workers based on queue depth
- Cleaning up temporary execution resources after every submission

---

## Database Design

The system requires persistence for information such as:

```text
Problem
TestCase
Submission
ExecutionResult
```

A submission can be associated with a problem and its execution result.

The database is primarily responsible for storing submission metadata and results rather than large temporary execution artifacts.

H2 can be used for development and testing, while PostgreSQL can be used for a production deployment.

---

## API Design

The backend exposes REST endpoints for interacting with the runtime environment.

The core submission workflow includes:

```http
POST /api/submissions
```

A submission contains information such as:

```json
{
  "problemId": 1,
  "language": "java",
  "code": "public class Main { ... }"
}
```

The API returns a submission identifier that can later be used to retrieve the execution result.

For example:

```http
GET /api/submissions/{id}
```

Possible submission states include:

```text
PENDING
RUNNING
ACCEPTED
WRONG_ANSWER
COMPILE_ERROR
RUNTIME_ERROR
TIME_LIMIT_EXCEEDED
MEMORY_LIMIT_EXCEEDED
SYSTEM_ERROR
```

---

## Development Approach

I am prioritizing a reliable working core over implementing every possible production feature.

The initial implementation focuses on:

1. Code submission
2. Compilation
3. Execution
4. Timeout handling
5. Output comparison
6. Verdict generation
7. Concurrent execution
8. Persistence
9. Reproducible deployment

More advanced isolation and distributed infrastructure are treated as extensions.

---

## Engineering Trade-offs

I prioritized a reliable working core while maintaining a clear path toward a stronger production architecture.

I chose a lightweight process-based execution model initially to reduce infrastructure complexity and focus on implementing and validating the core compilation, execution and judging pipeline.

Per-submission container isolation provides a stronger security boundary, but introduces additional infrastructure and deployment complexity. Therefore, I treated containerized execution as the next isolation layer for a production deployment rather than making it a dependency of the initial implementation.

This is a deliberate engineering trade-off. The initial execution model is not intended to be equivalent to a production-grade sandbox for arbitrary untrusted code.

For a production system, I would use stronger isolation with containers or a dedicated sandboxing technology, combined with strict CPU, memory, process, filesystem and network restrictions.

---

## Known Limitations

The initial implementation does not attempt to provide a fully production-grade security boundary.

In particular:

- Per-submission container isolation is not part of the initial implementation
- Distributed job queues are not initially required
- Advanced CPU and memory accounting may be limited
- Multi-language support is optional and may not be included
- Production-scale worker orchestration is outside the MVP
- Advanced monitoring and observability are not part of the initial implementation

These limitations are documented explicitly so that the system architecture and future improvements remain clear.

---

## Reproducibility

The project is designed to be reproducible by documenting:

- Required Java version
- Maven configuration
- Database configuration
- Runtime requirements
- Environment variables
- Build instructions
- Execution instructions

The final deployment configuration will be documented so that the project can be reproduced in a cloud environment.

---

## Future Improvements

- Per-submission container isolation
- Multi-language support
- Distributed message queue
- Horizontally scalable execution workers
- Stronger filesystem isolation
- Network isolation
- Advanced CPU and memory accounting
- Authentication and authorization
- Monitoring and observability
- Submission history and analytics
- Better resource usage measurement
- Automatic worker autoscaling

---

## Testing Strategy

The execution engine will be tested against both valid and abnormal submissions.

Important test cases include:

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

### Excessive Memory Usage

Expected:

```text
MEMORY_LIMIT_EXCEEDED
```

### Excessive Output

Expected:

```text
Execution terminated due to output/resource limit
```

These tests are intended to verify that the execution environment does not depend only on normal program behaviour.

---

## Status

🚧 Project under development.