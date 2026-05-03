# SonarQube Flaws — Solutions for OmniCharge (Java/Spring Boot)

> This document maps each of the 20 SonarQube exercises to the OmniCharge microservices architecture.
> For each exercise: the flaw is identified, its impact is explained, and the corrected code for OmniCharge is provided.

---

## Exercise 1: SQL Injection by String Concatenation

**Category**: Vulnerability  
**Observed flaw**: Building SQL queries via string concatenation allows malicious SQL execution.

**Impact in a microservice**: An attacker could extract, modify, or delete data from product/order/customer databases.

**OmniCharge Status**: ✅ **Already safe** — All services use Spring Data JPA with repository methods (`findById`, `findByUsername`, etc.), which use parameterized queries internally.

```java
// OmniCharge uses JPA Repository — no raw SQL
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    Optional<Transaction> findByRechargeId(Long rechargeId);
    List<Transaction> findByUsernameOrderByCreatedAtDesc(String username);
}
```

**Principle violated**: OWASP A03:2021 Injection — always use parameterized queries or ORM frameworks.

---

## Exercise 2: Hard-Coded Secrets

**Category**: Vulnerability  
**Observed flaw**: API keys or passwords embedded in source code leak through repositories and logs.

**Impact in a microservice**: Credentials committed to version control can be harvested by attackers.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — hard-coded in source code
private const String ApiKey = "sk-live-123456";

// ✓ CORRECT — externalized to application.properties / environment variables
@Value("${jwt.secret}")
private String jwtSecret;
```

**OmniCharge Status**: JWT secrets are in `application.properties` (not hard-coded in Java source). In production, these should be injected via environment variables or a secret manager.

**Principle violated**: CWE-798 Use of Hard-coded Credentials.

---

## Exercise 3: Possible Null Reference Dereference

**Category**: Bug  
**Observed flaw**: Dereferencing nested objects without null checks causes runtime crashes.

**Impact in a microservice**: A single null pointer can crash an API handler and return 500 errors.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — no null check
return customer.Address.City.ToUpper();

// ✓ CORRECT — null-safe access (Java equivalent)
public String getCustomerCity(Customer customer) {
    if (customer == null || customer.getAddress() == null || customer.getAddress().getCity() == null) {
        return "UNKNOWN";
    }
    return customer.getAddress().getCity().toUpperCase();
}
```

**OmniCharge Implementation** (PaymentServiceImpl.java):
```java
// Fixed: Null-safe check before accessing transaction in retry logic
Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
if (transaction == null) {
    log.error("Transaction not found for async retry: id={}", transactionId);
    return;
}
```

**Principle violated**: CWE-476 NULL Pointer Dereference.

---

## Exercise 4: Empty Catch Block

**Category**: Code Smell / Bug  
**Observed flaw**: Catching an exception and doing nothing hides failures.

**Impact in a microservice**: Silent failures make production troubleshooting impossible.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — empty catch block
} catch (Exception ex) {
    SecurityContextHolder.clearContext();
}

// ✓ CORRECT — log the error before clearing context
} catch (Exception ex) {
    log.warn("JWT authentication failed for request to {}: {}",
            request.getRequestURI(), ex.getMessage());
    SecurityContextHolder.clearContext();
}
```

**OmniCharge Implementation**: Fixed in `JwtAuthFilter.java` across all 5 services — added `Logger` and logging statement to every catch block.

**Principle violated**: SonarQube rule java:S108 — Nested blocks of code should not be left empty.

---

## Exercise 5: Overly Generic Exception Handling

**Category**: Code Smell  
**Observed flaw**: Catching `Exception` for every error path prevents meaningful handling.

**Impact in a microservice**: Validation errors, domain errors, and infrastructure errors all get the same treatment.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — one catch for everything
} catch (Exception ex) {
    return BadRequest(ex.Message);
}

// ✓ CORRECT — specific handlers for different exception types
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
}

@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    log.error("Unexpected error occurred", ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.");
}
```

**OmniCharge Implementation**: `GlobalExceptionHandler.java` in all services now handles `RuntimeException` and generic `Exception` separately. The user-service additionally handles `InvalidCredentialsException`, `UserAlreadyExistsException`, `ResourceNotFoundException`, and `MethodArgumentNotValidException`.

**Principle violated**: SonarQube rule java:S2221 — "Exception" should not be caught when not required by called methods.

---

## Exercise 6: Exposing Internal Exception Details

**Category**: Vulnerability  
**Observed flaw**: Returning `ex.ToString()` to the client leaks stack traces and implementation details.

**Impact in a microservice**: Attackers gain knowledge of framework versions, class paths, and database structure.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — exposes internals
error.put("details", ex.getMessage());

// ✓ CORRECT — log server-side, return safe message
log.error("Unexpected error occurred", ex);
error.put("message", "An unexpected error occurred. Please try again later.");
```

**OmniCharge Implementation**: Fixed in `GlobalExceptionHandler.java` across all 5 services — generic `Exception` handler now logs the full stack trace server-side and returns only a safe user-facing message.

**Principle violated**: CWE-209 Generation of Error Message Containing Sensitive Information.

---

## Exercise 7: Logging Sensitive Data

**Category**: Vulnerability  
**Observed flaw**: Passwords, tokens, and personal data written to logs.

**Impact in a microservice**: Logs are often stored in centralized systems accessible to many people.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — logging password and token
logger.info("Login request: {} / {}", request.Email, request.Password);
logger.info("JWT Token: {}", token);

// ✓ CORRECT — log only non-sensitive identifiers
logger.info("Login request received for user: {}", request.getEmail());
logger.debug("Authentication successful for user: {}", request.getEmail());
```

**OmniCharge Status**: ✅ **Already safe** — No passwords or raw JWT tokens are logged. Only usernames and non-sensitive IDs are logged.

**Principle violated**: CWE-532 Insertion of Sensitive Information into Log File.

---

## Exercise 8: Blocking Async Calls with .Result

**Category**: Bug (Performance)  
**Observed flaw**: `.Result` or `.Wait()` blocks threads and can cause deadlocks.

**Impact in a microservice**: Thread starvation under load causes cascading failures.

**Java equivalent issue and fix**:
```java
// ✗ WRONG (Java equivalent) — blocking an async call
CompletableFuture<Data> future = service.getDataAsync();
Data data = future.get(); // blocks the thread

// ✓ CORRECT — use @Async and proper async patterns
@Async
public void updateRechargeStatusWithRetry(Long rechargeId, Long transactionId) {
    // Runs in a separate thread pool, does not block the request thread
}
```

**OmniCharge Status**: ✅ Uses `@Async` properly in `PaymentServiceImpl.updateRechargeStatusWithRetry()`.

---

## Exercise 9: Improper HttpClient Usage

**Category**: Bug (Resource Leak)  
**Observed flaw**: Creating a new `HttpClient` per request exhausts sockets.

**OmniCharge Status**: ✅ **Not applicable** — Uses Spring Cloud OpenFeign which manages HTTP connections via a shared connection pool internally.

---

## Exercise 10: Undisposed IDisposable Resource

**Category**: Bug (Resource Leak)  
**Observed flaw**: Not closing streams/connections causes memory and handle leaks.

**Java equivalent and fix**:
```java
// ✗ WRONG
var reader = new StreamReader(path);
return reader.ReadToEnd();

// ✓ CORRECT (Java) — use try-with-resources
try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
    return reader.lines().collect(Collectors.joining("\n"));
}
```

**OmniCharge Status**: ✅ **Not applicable** — Spring managed resources (JPA EntityManager, RabbitMQ connections) are handled by the framework.

---

## Exercise 11: Path Traversal Risk

**Category**: Vulnerability  
**Observed flaw**: Using user-supplied file names directly allows access to unintended files.

**Java equivalent and fix**:
```java
// ✗ WRONG
var path = "/data/invoices/" + fileName;

// ✓ CORRECT — validate and sanitize
public String readInvoice(String fileName) {
    if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
        throw new SecurityException("Invalid file name");
    }
    Path path = Paths.get("/data/invoices").resolve(fileName).normalize();
    if (!path.startsWith("/data/invoices")) {
        throw new SecurityException("Path traversal detected");
    }
    return Files.readString(path);
}
```

**OmniCharge Status**: ✅ **Not applicable** — No file system operations exist in the microservices.

---

## Exercise 12: OS Command Injection

**Category**: Vulnerability  
**Observed flaw**: Passing user input into shell commands allows arbitrary command execution.

**OmniCharge Status**: ✅ **Not applicable** — No shell/process commands are executed.

---

## Exercise 13: Weak Random Number Generation

**Category**: Vulnerability  
**Observed flaw**: `System.Random` is predictable for security-sensitive values.

**Java equivalent and fix**:
```java
// ✗ WRONG
Random random = new Random();
return String.valueOf(random.nextInt(900000) + 100000);

// ✓ CORRECT — use SecureRandom
SecureRandom secureRandom = new SecureRandom();
return String.valueOf(secureRandom.nextInt(900000) + 100000);
```

**OmniCharge Status**: ✅ Uses `UUID.randomUUID()` for transaction IDs which is cryptographically secure.

---

## Exercise 14: Weak Hashing Algorithm

**Category**: Vulnerability  
**Observed flaw**: MD5 is not suitable for password hashing.

**OmniCharge Status**: ✅ **Already safe** — Uses Spring Security's `BCryptPasswordEncoder` for password hashing.

---

## Exercise 15: Disabled SSL Certificate Validation

**Category**: Vulnerability  
**Observed flaw**: Accepting any certificate defeats TLS protection.

**OmniCharge Status**: ✅ **Not applicable** — No custom SSL handlers; uses default JVM trust store.

---

## Exercise 16: Insecure Deserialization

**Category**: Vulnerability  
**Observed flaw**: Deserializing untrusted binary payloads allows malicious object loading.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — unnecessary Serializable + binary deserialization
public class PaymentResultMessage implements Serializable { ... }

// ✓ CORRECT — remove Serializable (Jackson JSON handles serialization)
public class PaymentResultMessage { ... }
```

**OmniCharge Implementation**: Removed `implements Serializable` from:
- `PaymentResultMessage` (payment-service + notification-service)
- `RechargeEventMessage` (payment-service + recharge-service + notification-service)

RabbitMQ uses `Jackson2JsonMessageConverter`, not Java binary serialization.

**Principle violated**: CWE-502 Deserialization of Untrusted Data.

---

## Exercise 17: High Cognitive Complexity

**Category**: Maintainability  
**Observed flaw**: Long methods with heavy nesting are hard to test and review.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — deeply nested logic
if (order != null) {
    if (order.Items != null) {
        foreach (var item in order.Items) {
            if (item.Quantity > 0) {
                if (item.Price > 0) { ... }
            }
        }
    }
}

// ✓ CORRECT — use guard clauses and extract methods
public decimal processOrder(Order order) {
    if (order == null || order.getItems() == null) return BigDecimal.ZERO;
    return order.getItems().stream()
        .filter(item -> item.getQuantity() > 0 && item.getPrice() > 0)
        .map(item -> calculateItemTotal(item, order.getCustomerType()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**OmniCharge Implementation**: Extracted `handleRetryExhaustion()` and `publishRefundNotification()` as separate methods from the large `updateRechargeStatusWithRetry()` method in `PaymentServiceImpl`.

**Principle violated**: SonarQube rule java:S3776 — Cognitive Complexity should not be too high.

---

## Exercise 18: Duplicate Code Across Services

**Category**: Maintainability  
**Observed flaw**: Repeated logic across services causes inconsistent behavior.

**Corrected approach for OmniCharge**:
```java
// ✗ WRONG — same buildErrorResponse in every GlobalExceptionHandler
Map<String, String> error = new HashMap<>();
error.put("error", ex.getMessage());
return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

// ✓ CORRECT — extracted common utility method
private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
    Map<String, Object> error = new LinkedHashMap<>();
    error.put("timestamp", LocalDateTime.now().toString());
    error.put("status", status.value());
    error.put("error", status.getReasonPhrase());
    error.put("message", message);
    return ResponseEntity.status(status).body(error);
}
```

**OmniCharge Implementation**: All `GlobalExceptionHandler` classes now use a shared `buildErrorResponse()` method with structured, consistent error formatting.

**Principle violated**: DRY (Don't Repeat Yourself) principle.

---

## Exercise 19: Mutable Static Shared State

**Category**: Bug (Concurrency)  
**Observed flaw**: Shared mutable static data causes race conditions in multi-threaded services.

**Java equivalent and fix**:
```java
// ✗ WRONG
public static int RequestCount = 0;
public void increment() { RequestCount++; }

// ✓ CORRECT — use AtomicInteger or proper synchronization
private static final AtomicInteger requestCount = new AtomicInteger(0);
public void increment() { requestCount.incrementAndGet(); }
```

**OmniCharge Status**: ✅ **Already safe** — No mutable static state. Only `static final Logger` instances exist (immutable).

---

## Exercise 20: Missing CancellationToken in I/O Calls

**Category**: Maintainability  
**Observed flaw**: External calls without cancellation support can hang indefinitely.

**Java equivalent and fix**:
```java
// ✗ WRONG — no timeout or interruption handling
Thread.sleep(retryIntervalSeconds * 1000);

// ✓ CORRECT — respect thread interruption
try {
    Thread.sleep(retryIntervalSeconds * 1000);
} catch (InterruptedException ie) {
    Thread.currentThread().interrupt();  // Restore interrupt flag
    log.warn("Retry thread interrupted for rechargeId={}", rechargeId);
    return;
}
```

**OmniCharge Implementation**: ✅ `InterruptedException` is properly handled in `PaymentServiceImpl.updateRechargeStatusWithRetry()` — the interrupt flag is restored and the method returns cleanly.

**Principle violated**: Java best practice — never swallow `InterruptedException`.
