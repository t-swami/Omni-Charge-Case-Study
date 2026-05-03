# OmniCharge — Software Requirements Specification (SRS)

**Document Version:** 1.0  
**Project Name:** OmniCharge — Mobile Recharge Platform  
**Prepared By:** Sprint Team  
**Date:** May 2026  

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements for the OmniCharge Mobile Recharge Platform. It serves as a contract between the development team and stakeholders.

### 1.2 Project Overview
OmniCharge is a web-based mobile recharge application that allows users to recharge prepaid mobile numbers through multiple payment methods. The system is built on a microservices architecture to ensure scalability and resilience.

### 1.3 Definitions and Acronyms

| Term | Definition |
|---|---|
| JWT | JSON Web Token — A compact, URL-safe token for authentication |
| API | Application Programming Interface |
| CRUD | Create, Read, Update, Delete |
| DTO | Data Transfer Object |
| CORS | Cross-Origin Resource Sharing |
| CSRF | Cross-Site Request Forgery |
| RBAC | Role-Based Access Control |

---

## 2. Functional Requirements

### 2.1 User Management Module

| ID | Requirement | Priority |
|---|---|---|
| FR-001 | The system shall allow new users to register with username, email, password, full name, and phone number. | High |
| FR-002 | The system shall encrypt user passwords using BCrypt before storing them in the database. | High |
| FR-003 | The system shall generate a JWT token upon successful login. | High |
| FR-004 | The system shall support two roles: ROLE_USER and ROLE_ADMIN. | High |
| FR-005 | The system shall allow admin registration using a pre-configured admin secret key. | Medium |
| FR-006 | The system shall allow users to view and edit their profile. | Medium |
| FR-007 | The system shall allow users to change their password. | Medium |
| FR-008 | The system shall allow admins to view all registered users. | Medium |
| FR-009 | The system shall allow admins to promote a regular user to admin. | Medium |

### 2.2 Wallet Module

| ID | Requirement | Priority |
|---|---|---|
| FR-010 | The system shall maintain a wallet balance for each user. | High |
| FR-011 | The system shall allow users to top up their wallet using UPI. | High |
| FR-012 | The system shall allow users to pay for recharges using their wallet balance. | High |
| FR-013 | The system shall prevent wallet payments when balance is insufficient. | High |
| FR-014 | The system shall automatically refund wallet balance when a recharge fails after payment. | High |

### 2.3 Operator & Plan Management Module

| ID | Requirement | Priority |
|---|---|---|
| FR-015 | The system shall allow admins to create, update, patch, and delete operators. | High |
| FR-016 | The system shall allow admins to create, update, patch, and delete recharge plans. | High |
| FR-017 | The system shall support operator types: MOBILE, DTH, BROADBAND. | Medium |
| FR-018 | The system shall support operator statuses: ACTIVE, INACTIVE. | Medium |
| FR-019 | The system shall allow users to browse operators and their plans. | High |
| FR-020 | The system shall allow filtering operators by status and type. | Medium |
| FR-021 | The system shall allow filtering plans by operator, category, and status. | Medium |
| FR-022 | The system shall cache operator and plan data using Redis for performance. | Medium |

### 2.4 Recharge Module

| ID | Requirement | Priority |
|---|---|---|
| FR-023 | The system shall validate mobile numbers (must be 10-digit Indian numbers starting with 6-9). | High |
| FR-024 | The system shall validate that the selected operator and plan are ACTIVE before initiating recharge. | High |
| FR-025 | The system shall create a recharge record with PENDING status upon initiation. | High |
| FR-026 | The system shall publish a recharge event to RabbitMQ for asynchronous payment processing. | High |
| FR-027 | The system shall allow users to cancel PENDING recharges. | Medium |
| FR-028 | The system shall allow users to view their recharge history. | Medium |
| FR-029 | The system shall allow admins to view all recharges and filter by status or mobile number. | Medium |

### 2.5 Payment Module

| ID | Requirement | Priority |
|---|---|---|
| FR-030 | The system shall support four payment methods: CARD, UPI, NETBANKING, and WALLET. | High |
| FR-031 | The system shall validate card numbers (16 digits), CVV (3 digits), and expiry (MM/YY). | High |
| FR-032 | The system shall validate UPI IDs (format: name@bank). | High |
| FR-033 | The system shall validate bank codes against a pre-configured list. | Medium |
| FR-034 | The system shall validate net banking account numbers (9-18 digits). | Medium |
| FR-035 | The system shall generate a unique transaction ID for every payment. | High |
| FR-036 | The system shall update the recharge status after payment using a retry mechanism. | High |
| FR-037 | The system shall retry recharge status updates up to 6 times with 30-second intervals. | High |
| FR-038 | The system shall mark transactions as REFUND_PENDING if all retries are exhausted. | High |

### 2.6 Notification Module

| ID | Requirement | Priority |
|---|---|---|
| FR-039 | The system shall send email notifications for successful payments. | High |
| FR-040 | The system shall send email notifications for failed payments. | High |
| FR-041 | The system shall send email notifications for refund-pending transactions. | Medium |
| FR-042 | Notifications shall be sent asynchronously via RabbitMQ. | High |

---

## 3. Non-Functional Requirements

### 3.1 Performance

| ID | Requirement |
|---|---|
| NFR-001 | The system shall cache frequently accessed data (operators, plans) using Redis to reduce database load. |
| NFR-002 | API response time shall be under 2 seconds for standard CRUD operations. |
| NFR-003 | The system shall support asynchronous processing for payment and notification workflows. |

### 3.2 Security

| ID | Requirement |
|---|---|
| NFR-004 | All passwords shall be stored using BCrypt encryption. |
| NFR-005 | All protected APIs shall require a valid JWT token. |
| NFR-006 | The API Gateway shall validate JWT tokens before forwarding requests. |
| NFR-007 | Admin-only endpoints shall enforce ROLE_ADMIN authorization. |
| NFR-008 | The system shall use HTTPS-ready configurations. |

### 3.3 Reliability

| ID | Requirement |
|---|---|
| NFR-009 | The system shall implement retry logic for inter-service communication failures. |
| NFR-010 | The system shall implement automatic refund for wallet payments on recharge failure. |
| NFR-011 | RabbitMQ queues shall be durable to survive server restarts. |

### 3.4 Scalability

| ID | Requirement |
|---|---|
| NFR-012 | Each microservice shall be independently deployable and scalable. |
| NFR-013 | The system shall use Eureka for dynamic service discovery. |
| NFR-014 | The system shall be containerized using Docker for consistent deployment. |

### 3.5 Maintainability

| ID | Requirement |
|---|---|
| NFR-015 | The codebase shall achieve a minimum of 80% test coverage (measured by JaCoCo). |
| NFR-016 | Code quality shall be monitored using SonarQube. |
| NFR-017 | The system shall use centralized configuration via Config Server. |
| NFR-018 | The system shall implement distributed tracing via Zipkin. |

---

## 4. User Stories

### 4.1 User Stories

| ID | As a... | I want to... | So that... |
|---|---|---|---|
| US-001 | User | register an account | I can use the recharge platform |
| US-002 | User | log in with my credentials | I can access my dashboard |
| US-003 | User | browse mobile operators | I can find my carrier |
| US-004 | User | view available recharge plans | I can choose a plan |
| US-005 | User | initiate a mobile recharge | my mobile gets recharged |
| US-006 | User | pay using Card/UPI/NetBanking/Wallet | I can complete my recharge |
| US-007 | User | add money to my wallet | I can use wallet for future recharges |
| US-008 | User | view my recharge history | I can track past recharges |
| US-009 | User | view my transaction history | I can see payment details |
| US-010 | User | cancel a pending recharge | I can stop an unwanted recharge |
| US-011 | User | receive email notifications | I get confirmation of my payment |

### 4.2 Admin Stories

| ID | As an... | I want to... | So that... |
|---|---|---|---|
| US-012 | Admin | add new operators | users can see new carriers |
| US-013 | Admin | manage recharge plans | users have updated plan options |
| US-014 | Admin | view all users | I can monitor the user base |
| US-015 | Admin | promote users to admin | I can delegate management tasks |
| US-016 | Admin | view all recharges | I can monitor system activity |
| US-017 | Admin | filter transactions by status | I can investigate issues |

---

## 5. Acceptance Criteria

| Feature | Criteria |
|---|---|
| User Registration | User can register with unique username and email; password is encrypted; JWT token is returned. |
| Login | Correct credentials return a JWT token; incorrect credentials return an error message. |
| Recharge Initiation | Mobile number must be valid (10 digits, starts with 6-9); operator and plan must be ACTIVE; recharge record is created as PENDING. |
| Payment | Valid payment details result in SUCCESS status; invalid details result in FAILED status with a reason. |
| Wallet | Insufficient balance prevents payment; wallet balance updates correctly after top-up and deduction. |
| Notification | Email is sent to the user after payment completion (success or failure). |
| Circuit Breaker | Failed recharge status updates are retried up to 6 times; exhaustion triggers REFUND_PENDING status. |
