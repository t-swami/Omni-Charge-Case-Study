# OmniCharge — Testing & Quality Assurance Document

**Document Version:** 1.0  
**Project Name:** OmniCharge — Mobile Recharge Platform  
**Prepared By:** Sprint Team  
**Date:** May 2026  

---

## 1. Testing Strategy

### 1.1 Testing Levels

| Level | Tool | Scope | Coverage Target |
|---|---|---|---|
| Unit Testing | JUnit 5 + Mockito | Individual methods and classes | 80% |
| Integration Testing | Spring Boot Test | Service layer with mocked dependencies | Key flows |
| API Testing | Swagger UI | REST endpoint verification | All endpoints |
| Code Quality | SonarQube | Bugs, vulnerabilities, code smells | Quality Gate Pass |

### 1.2 Testing Framework Stack
- **JUnit 5:** Test runner and assertion framework.
- **Mockito:** Mocking framework for isolating units under test.
- **Spring Boot Test:** Test context support and annotations.
- **JaCoCo:** Code coverage measurement tool.
- **SonarQube:** Code quality and security analysis dashboard.

---

## 2. Unit Testing Approach

### 2.1 Pattern: Arrange-Act-Assert (Given-When-Then)

Every test case follows this three-step structure:

```java
@Test
void testLoginUser_Success() {
    // ARRANGE (Given): Set up test data and mock behaviors
    User mockUser = new User();
    mockUser.setUsername("rahul");
    mockUser.setPassword(encodedPassword);
    when(userRepository.findByUsername("rahul")).thenReturn(mockUser);
    when(passwordEncoder.matches("password123", encodedPassword)).thenReturn(true);

    // ACT (When): Execute the method under test
    AuthResponse result = userService.loginUser(loginRequest);

    // ASSERT (Then): Verify the expected outcome
    assertNotNull(result);
    assertEquals("rahul", result.getUsername());
    verify(jwtUtil, times(1)).generateToken(anyString(), anyString(), anyString());
}
```

### 2.2 Key Testing Annotations

| Annotation | Purpose |
|---|---|
| `@Test` | Marks a method as a test case |
| `@ExtendWith(MockitoExtension.class)` | Enables Mockito mocking in tests |
| `@Mock` | Creates a mock (fake) object |
| `@InjectMocks` | Creates the real object with mocks injected |
| `@BeforeEach` | Runs setup code before each test |
| `@DisplayName` | Provides a human-readable test name |

### 2.3 Key Assertion Methods

| Method | Purpose |
|---|---|
| `assertEquals(expected, actual)` | Checks two values are equal |
| `assertNotNull(object)` | Checks object is not null |
| `assertTrue(condition)` | Checks condition is true |
| `assertThrows(Exception.class, () -> ...)` | Checks that code throws expected exception |
| `verify(mock, times(n)).method()` | Checks mock method was called n times |

---

## 3. Test Coverage by Service

### 3.1 User Service Tests

| Test Class | Methods Tested | Key Scenarios |
|---|---|---|
| `UserServiceImplTest` | register, loginUser, loginAdmin, getUserProfile, getAllUsers, promoteToAdmin, changePassword, updateWalletBalance | Happy path, duplicate user, wrong password, user not found, insufficient balance |
| `UserControllerTest` | All REST endpoints | Request validation, response codes, role-based access |
| `JwtUtilTest` | generateToken, validateToken, extractUsername, extractRole | Valid token, expired token, tampered token |
| `JwtAuthFilterTest` | doFilterInternal | Valid token flow, missing token flow |

### 3.2 Operator Service Tests

| Test Class | Methods Tested | Key Scenarios |
|---|---|---|
| `OperatorServiceImplTest` | addOperator, updateOperator, deleteOperator, getAll, getByStatus | CRUD operations, cache eviction, not found |
| `RechargePlanServiceImplTest` | addPlan, updatePlan, patchPlan, deletePlan, getByOperator | CRUD operations, partial update, operator not found |

### 3.3 Recharge Service Tests

| Test Class | Methods Tested | Key Scenarios |
|---|---|---|
| `RechargeServiceImplTest` | initiateRecharge, cancelRecharge, updateStatus, getHistory | Valid recharge, invalid mobile, inactive plan, cancel non-pending, access denied |
| `RechargeServiceImplExtendedTest` | Edge cases | Boundary conditions, concurrent access |

### 3.4 Payment Service Tests

| Test Class | Methods Tested | Key Scenarios |
|---|---|---|
| `PaymentServiceImplTest` | processPayment, makePayment, topUpWallet | Duplicate event, access denied, already processed |
| `PaymentServiceImplExtendedTest` | Retry and refund logic | Circuit breaker exhaustion, wallet refund |
| `DummyPaymentGatewayServiceTest` | Card, UPI, NetBanking, Wallet validation | Valid/invalid card, invalid UPI, unsupported bank, wallet type |

---

## 4. Test Cases

### 4.1 User Registration Test Cases

| TC ID | Test Case | Input | Expected Result |
|---|---|---|---|
| TC-001 | Successful registration | Valid username, email, password | User created, JWT returned |
| TC-002 | Duplicate username | Existing username | RuntimeException: "Username already exists" |
| TC-003 | Duplicate email | Existing email | RuntimeException: "Email already exists" |

### 4.2 Login Test Cases

| TC ID | Test Case | Input | Expected Result |
|---|---|---|---|
| TC-004 | Successful user login | Valid credentials | JWT token returned |
| TC-005 | Wrong password | Valid username, wrong password | RuntimeException: "Invalid password" |
| TC-006 | User not found | Non-existent username | RuntimeException: "User not found" |
| TC-007 | Admin login as user | Admin credentials on user login | RuntimeException or role check failure |

### 4.3 Recharge Test Cases

| TC ID | Test Case | Input | Expected Result |
|---|---|---|---|
| TC-008 | Valid recharge | Valid mobile, active operator/plan | Recharge created with PENDING status |
| TC-009 | Invalid mobile number | "12345" (5 digits) | RuntimeException: "Invalid mobile number" |
| TC-010 | Inactive operator | Valid mobile, inactive operator | RuntimeException: "Operator is inactive" |
| TC-011 | Inactive plan | Valid mobile, inactive plan | RuntimeException: "Plan is inactive" |
| TC-012 | Plan-operator mismatch | Plan from different operator | RuntimeException: "Plan does not belong to operator" |

### 4.4 Payment Test Cases

| TC ID | Test Case | Input | Expected Result |
|---|---|---|---|
| TC-013 | Successful card payment | Valid 16-digit card, valid CVV | Transaction SUCCESS |
| TC-014 | Invalid card number | "1234" (4 digits) | Payment FAILED: "Invalid card number" |
| TC-015 | Successful UPI payment | Valid UPI (name@bank) | Transaction SUCCESS |
| TC-016 | Invalid UPI format | "noatsign" | Payment FAILED: "Invalid UPI format" |
| TC-017 | Successful wallet payment | Sufficient balance | Transaction SUCCESS, balance deducted |
| TC-018 | Insufficient wallet balance | Balance < amount | RuntimeException: "Insufficient balance" |
| TC-019 | Wallet top-up | Valid UPI, amount > 0 | Balance increased |

### 4.5 Circuit Breaker Test Cases

| TC ID | Test Case | Input | Expected Result |
|---|---|---|---|
| TC-020 | Retry success on attempt 2 | Recharge service down then up | Status updated on retry |
| TC-021 | All retries exhausted | Recharge service permanently down | Transaction marked REFUND_PENDING |
| TC-022 | Wallet auto-refund | Wallet payment + all retries fail | Wallet balance restored |

---

## 5. Code Coverage Report

### 5.1 Coverage Targets

| Service | Target | Measurement Tool |
|---|---|---|
| User Service | ≥ 80% | JaCoCo |
| Operator Service | ≥ 80% | JaCoCo |
| Recharge Service | ≥ 80% | JaCoCo |
| Payment Service | ≥ 80% | JaCoCo |
| Notification Service | ≥ 80% | JaCoCo |

### 5.2 Running Tests

```bash
# Run tests for a specific service
cd user-service
mvn clean test

# Generate JaCoCo coverage report
mvn jacoco:report

# Run SonarQube analysis
mvn sonar:sonar -Dsonar.projectKey=user-service -Dsonar.host.url=http://localhost:9000
```

---

## 6. SonarQube Quality Gates

### 6.1 Quality Gate Criteria

| Metric | Threshold | Description |
|---|---|---|
| Code Coverage | ≥ 80% | Percentage of code executed by tests |
| Duplicated Lines | ≤ 3% | Amount of copy-pasted code |
| Bugs | 0 (Critical) | Code that is clearly wrong |
| Vulnerabilities | 0 (Critical) | Security-related issues |
| Code Smells | Monitored | Maintainability issues |

### 6.2 SonarQube Dashboard
- URL: `http://localhost:9000`
- Each service is analyzed as a separate SonarQube project.
- Quality Gate status: PASSED indicates all criteria are met.

---

## 7. API Testing (Swagger)

### 7.1 Swagger UI Access
Each service provides a Swagger UI for interactive API testing:

| Service | Swagger URL |
|---|---|
| User Service | `http://localhost:8081/swagger-ui/index.html` |
| Operator Service | `http://localhost:8082/swagger-ui/index.html` |
| Recharge Service | `http://localhost:8083/swagger-ui/index.html` |
| Payment Service | `http://localhost:8084/swagger-ui/index.html` |
| Gateway (Aggregated) | `http://localhost:8080/swagger-ui.html` |

### 7.2 Testing Workflow
1. Open the Swagger UI for the relevant service.
2. Click "Authorize" and paste a valid JWT token.
3. Expand the desired endpoint.
4. Fill in the request body or path parameters.
5. Click "Execute."
6. Verify the response status code and body.
