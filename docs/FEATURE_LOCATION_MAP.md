# Feature Location Map

This document points each important OmniCharge feature to the exact frontend and backend files where it is implemented.

Use this file when you ask: "Where is this feature written in the project?"

## Auth And User Features

| Feature | Frontend | API | Backend files |
|---|---|---|---|
| User register | `omnicharge-frontend/src/app/pages/user-register/` and `services/auth.service.ts` | `POST /api/auth/register` | `user-service/.../controller/AuthController.java`, `dto/RegisterRequest.java`, `service/UserServiceImpl.java`, `entity/User.java`, `repository/UserRepository.java` |
| Admin register | `omnicharge-frontend/src/app/pages/admin-register/` and `services/auth.service.ts` | `POST /api/auth/register-admin` | `AuthController.java`, `dto/AdminRegisterRequest.java`, `UserServiceImpl.registerAdmin()`, `application.properties` `app.admin.secret-key` |
| User login | `omnicharge-frontend/src/app/pages/user-login/` and `services/auth.service.ts` | `POST /api/auth/user/login` | `AuthController.java`, `dto/LoginRequest.java`, `UserServiceImpl.loginUser()`, `security/JwtUtil.java` |
| Admin login | `omnicharge-frontend/src/app/pages/admin-login/` and `services/auth.service.ts` | `POST /api/auth/admin/login` | `AuthController.java`, `dto/LoginRequest.java`, `UserServiceImpl.loginAdmin()`, `security/JwtUtil.java` |
| Token storage | `services/auth.service.ts` | None | Stores `currentUser` and `token` cookies |
| Token on API calls | `interceptors/auth.interceptor.ts` | All protected APIs | Adds `Authorization` header |
| User page guard | `guards/auth.guard.ts`, `app.routes.ts` | None | Allows only logged-in users |
| Admin page guard | `guards/admin.guard.ts`, `app.routes.ts` | None | Allows only users with `ROLE_ADMIN` |
| Profile | `pages/profile/`, `services/user.service.ts` | `GET /api/users/profile` | `UserController.java`, `UserServiceImpl.getUserProfile()` |
| Change password | `pages/profile/`, `services/user.service.ts` | `PUT /api/users/change-password` | `UserController.java`, `dto/ChangePasswordRequest.java`, `UserServiceImpl.changePassword()` |
| View users | `pages/admin-dashboard/`, `services/user.service.ts` | `GET /api/users/all` | `UserController.java`, `UserServiceImpl.getAllUsers()`, `config/SecurityConfig.java` |
| Promote user | `pages/admin-dashboard/`, `services/user.service.ts` | `PUT /api/users/promote/{userId}` | `UserController.java`, `UserServiceImpl.promoteToAdmin()` |
| Wallet balance | `pages/payment/`, `services/user.service.ts` | `GET /api/users/profile/wallet` | `UserController.java`, `UserServiceImpl.getWalletBalance()` |
| Wallet update | Payment Service Feign client | `POST /api/users/profile/wallet/update` | `UserController.java`, `UserServiceImpl.updateWalletBalance()`, `payment-service/.../feign/UserServiceFeignClient.java` |

## Operator And Plan Features

| Feature | Frontend | API | Backend files |
|---|---|---|---|
| Create operator | `pages/admin-dashboard/`, `services/operator.service.ts` | `POST /api/operators` | `operator-service/.../controller/OperatorController.java`, `service/OperatorServiceImpl.java`, `entity/Operator.java`, `repository/OperatorRepository.java` |
| Update operator | `pages/admin-dashboard/`, `services/operator.service.ts` | `PUT /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.updateOperator()` |
| Patch operator | API supported | `PATCH /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.patchOperator()` |
| Delete operator | `pages/admin-dashboard/`, `services/operator.service.ts` | `DELETE /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.deleteOperator()` |
| List operators | `pages/recharge/`, `pages/admin-dashboard/`, `services/operator.service.ts` | `GET /api/operators` | `OperatorController.java`, `OperatorServiceImpl.getAllOperators()`, Redis cache |
| Operator by id | Feign and API | `GET /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.getOperatorById()`, `recharge-service/.../feign/OperatorFeignClient.java` |
| Operators by status | `pages/admin-dashboard/`, `services/operator.service.ts` | `GET /api/operators/status/{status}` | `OperatorController.java`, `OperatorServiceImpl.getOperatorsByStatus()` |
| Operators by type | `pages/admin-dashboard/`, `services/operator.service.ts` | `GET /api/operators/type/{type}` | `OperatorController.java`, `OperatorServiceImpl.getOperatorsByType()` |
| Create plan | `pages/admin-dashboard/`, `services/operator.service.ts` | `POST /api/plans` | `RechargePlanController.java`, `RechargePlanServiceImpl.addPlan()`, `entity/RechargePlan.java`, `repository/RechargePlanRepository.java` |
| Update plan | `pages/admin-dashboard/`, `services/operator.service.ts` | `PUT /api/plans/{id}` | `RechargePlanController.java`, `RechargePlanServiceImpl.updatePlan()` |
| Patch plan | API supported | `PATCH /api/plans/{id}` | `RechargePlanController.java`, `RechargePlanServiceImpl.patchPlan()` |
| Delete plan | `pages/admin-dashboard/`, `services/operator.service.ts` | `DELETE /api/plans/{id}` | `RechargePlanController.java`, `RechargePlanServiceImpl.deletePlan()` |
| List plans | `pages/admin-dashboard/`, `services/operator.service.ts` | `GET /api/plans` | `RechargePlanController.java`, `RechargePlanServiceImpl.getAllPlans()` |
| Plans by operator | `pages/recharge/`, `services/operator.service.ts` | `GET /api/plans/operator/{operatorId}` | `RechargePlanController.java`, `RechargePlanServiceImpl.getPlansByOperator()` |
| Active plans by operator | `services/operator.service.ts` | `GET /api/plans/operator/{operatorId}/active` | `RechargePlanController.java`, `RechargePlanServiceImpl.getActivePlansByOperator()` |
| Plans by category | `pages/admin-dashboard/`, `services/operator.service.ts` | `GET /api/plans/category/{category}` | `RechargePlanController.java`, `RechargePlanServiceImpl.getPlansByCategory()` |
| Redis caching | None | Same read APIs | `operator-service/.../config/CacheConfig.java`, `OperatorServiceImpl.java`, `RechargePlanServiceImpl.java` |

## Recharge Features

| Feature | Frontend | API or queue | Backend files |
|---|---|---|---|
| Load operators for recharge | `pages/recharge/recharge.component.ts` | `GET /api/operators` | Operator Service files |
| Load plans for selected operator | `pages/recharge/recharge.component.ts` | `GET /api/plans/operator/{operatorId}` | Operator Service files |
| Start recharge | `pages/recharge/`, `services/recharge.service.ts` | `POST /api/recharge/initiate` | `RechargeController.java`, `RechargeServiceImpl.initiateRecharge()` |
| Validate mobile number | Frontend form and backend service | Same API | Regex in `RechargeServiceImpl.initiateRecharge()` |
| Validate operator and plan | Backend | Feign to Operator Service | `recharge-service/.../feign/OperatorFeignClient.java` |
| Save recharge record | Backend | Same API | `RechargeRepository.java`, `RechargeRequest.java` |
| Publish payment event | Backend | RabbitMQ `payment.queue` | `RechargeEventPublisher.java`, `RabbitMQConfig.java`, `dto/RechargeEventMessage.java` |
| My recharge history | `pages/user-dashboard/`, `pages/history/` | `GET /api/recharge/my-history` | `RechargeController.java`, `RechargeServiceImpl.getMyRechargeHistory()` |
| Recharge by id | `pages/payment/` | `GET /api/recharge/{id}` | `RechargeController.java`, `RechargeServiceImpl.getRechargeById()` |
| Cancel recharge | `pages/user-dashboard/`, `pages/history/`, `pages/admin-dashboard/` | `PUT /api/recharge/{id}/cancel` | `RechargeController.java`, `RechargeServiceImpl.cancelRecharge()` |
| All recharges | `pages/admin-dashboard/` | `GET /api/recharge/all` | `RechargeController.java`, `RechargeServiceImpl.getAllRecharges()` |
| Recharges by status | `pages/admin-dashboard/` | `GET /api/recharge/status/{status}` | `RechargeController.java`, `RechargeServiceImpl.getRechargesByStatus()` |
| Recharges by mobile | `pages/admin-dashboard/` | `GET /api/recharge/mobile/{mobileNumber}` | `RechargeController.java`, `RechargeServiceImpl.getRechargesByMobile()` |
| Update recharge status | Payment Service | `PUT /api/recharge/update-status/{rechargeId}` | `RechargeController.java`, `RechargeServiceImpl.updateRechargeStatus()`, `payment-service/.../feign/RechargeServiceFeignClient.java` |

## Payment And Notification Features

| Feature | Frontend | API or queue | Backend files |
|---|---|---|---|
| Consume recharge event | None | RabbitMQ `payment.queue` | `payment-service/.../messaging/RechargeEventConsumer.java`, `PaymentServiceImpl.processPayment()` |
| Create pending transaction | None | From queue | `PaymentServiceImpl.processPayment()`, `TransactionRepository.java`, `Transaction.java` |
| Payment page | `pages/payment/` | `GET /api/recharge/{id}` and wallet APIs | `PaymentComponent` plus backend services |
| Pay by card/UPI/netbanking | `pages/payment/`, `services/payment.service.ts` | `POST /api/transactions/pay` | `TransactionController.java`, `PaymentServiceImpl.makePayment()`, `DummyPaymentGatewayService.java` |
| Pay by wallet | `pages/payment/` | `POST /api/transactions/pay` | `PaymentServiceImpl.makePayment()`, `UserServiceFeignClient.java`, `UserServiceImpl.updateWalletBalance()` |
| Top up wallet | `pages/payment/` | `POST /api/transactions/wallet/topup` | `TransactionController.java`, `PaymentServiceImpl.topUpWallet()`, `UserServiceFeignClient.java` |
| Update recharge after payment | None | Feign to Recharge Service | `PaymentServiceImpl.updateRechargeStatusWithRetry()`, `RechargeServiceFeignClient.java` |
| Retry on recharge update failure | None | Internal backend logic | `PaymentServiceImpl.updateRechargeStatusWithRetry()` |
| Refund pending | None | Internal backend logic and notification queue | `PaymentServiceImpl.handleRetryExhaustion()`, `publishRefundNotification()` |
| Publish notification | None | RabbitMQ `notification.queue` | `PaymentResultPublisher.java`, `PaymentServiceImpl.publishNotification()` |
| Consume notification | None | RabbitMQ `notification.queue` | `notification-service/.../messaging/NotificationConsumer.java` |
| Send email | None | SMTP/mail config | `notification-service/.../service/EmailService.java` |
| My transactions | `pages/history/`, `services/payment.service.ts` | `GET /api/transactions/my-transactions` | `TransactionController.java`, `PaymentServiceImpl.getMyTransactions()` |
| Transaction by transaction id | `services/payment.service.ts` | `GET /api/transactions/txn/{transactionId}` | `TransactionController.java`, `PaymentServiceImpl.getByTransactionId()` |
| Transaction by recharge id | `services/payment.service.ts` | `GET /api/transactions/recharge/{rechargeId}` | `TransactionController.java`, `PaymentServiceImpl.getByRechargeId()` |
| All transactions | `pages/admin-dashboard/` | `GET /api/transactions/all` | `TransactionController.java`, `PaymentServiceImpl.getAllTransactions()` |
| Transactions by status | `pages/admin-dashboard/` | `GET /api/transactions/status/{status}` | `TransactionController.java`, `PaymentServiceImpl.getTransactionsByStatus()` |
| Transactions by mobile | `pages/admin-dashboard/` | `GET /api/transactions/mobile/{mobileNumber}` | `TransactionController.java`, `PaymentServiceImpl.getTransactionsByMobile()` |

## Infrastructure Features

| Feature | Files |
|---|---|
| Docker startup | `docker-compose.yml`, each service `Dockerfile` |
| Database creation | `init-db.sql` |
| Environment variables | `.env.example`, `.env`, `docker-compose.yml`, service `application.properties` files |
| Service discovery | `eureka-server/`, service `application.properties` files |
| Config server | `config-server/`, service `spring.config.import` settings |
| Gateway routing | `api-gateway/src/main/resources/application.properties` |
| Gateway JWT filter | `api-gateway/src/main/java/com/omnicharge/api_gateway/filter/JwtAuthenticationFilter.java` |
| CORS | `api-gateway/src/main/java/com/omnicharge/api_gateway/config/CorsConfig.java` |
| Swagger | `SwaggerConfig.java` files and gateway Swagger properties |
| Zipkin tracing | `docker-compose.yml`, `management.zipkin.tracing.endpoint` properties |
| SonarQube | `docker-compose.yml`, `sonar-project.properties`, `sonar-scan.ps1` |
| Backend tests | `*/src/test/java/**` |
| Frontend tests | Angular test setup in `omnicharge-frontend/` |
