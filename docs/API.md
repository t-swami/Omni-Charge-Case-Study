# API Guide

This document lists the main API endpoints in OmniCharge.

For normal frontend use, call APIs through the API Gateway:

```text
http://localhost:8080
```

For example:

```text
POST http://localhost:8080/api/auth/user/login
```

## Authentication

Most APIs need a JWT token.

After login, the response returns a token. For protected APIs, send this header:

```text
Authorization: Bearer your-token-here
```

## Auth APIs

Base path:

```text
/api/auth
```

| Method | Path | Purpose | Login needed |
|---|---|---|---|
| POST | `/register` | Register normal user | No |
| POST | `/register-admin` | Register admin user | No, but admin secret is required in request |
| POST | `/user/login` | Login as normal user | No |
| POST | `/admin/login` | Login as admin | No |

Example user login body:

```json
{
  "username": "raju",
  "password": "password123"
}
```

## User APIs

Base path:

```text
/api/users
```

| Method | Path | Purpose | Common role |
|---|---|---|---|
| GET | `/profile` | Get logged-in user profile | User or admin |
| GET | `/all` | Get all users | Admin |
| PUT | `/promote/{userId}` | Promote a user to admin | Admin |
| PUT | `/change-password` | Change logged-in user's password | User or admin |
| GET | `/profile/wallet` | Get wallet balance | User or admin |
| POST | `/profile/wallet/update` | Update wallet balance | Internal or protected use |

## Operator APIs

Base path:

```text
/api/operators
```

| Method | Path | Purpose | Common role |
|---|---|---|---|
| POST | `/` | Create operator | Admin |
| PUT | `/{id}` | Replace operator details | Admin |
| PATCH | `/{id}` | Update some operator details | Admin |
| DELETE | `/{id}` | Delete operator | Admin |
| GET | `/{id}` | Get one operator | User or admin |
| GET | `/` | Get all operators | User or admin |
| GET | `/status/{status}` | Get operators by status | User or admin |
| GET | `/type/{type}` | Get operators by type | User or admin |

Example operator body:

```json
{
  "name": "Airtel",
  "type": "MOBILE",
  "status": "ACTIVE",
  "logoUrl": "https://example.com/airtel.png",
  "description": "Airtel mobile recharge"
}
```

## Recharge Plan APIs

Base path:

```text
/api/plans
```

| Method | Path | Purpose | Common role |
|---|---|---|---|
| POST | `/` | Create recharge plan | Admin |
| PUT | `/{id}` | Replace plan details | Admin |
| PATCH | `/{id}` | Update some plan details | Admin |
| DELETE | `/{id}` | Delete plan | Admin |
| GET | `/{id}` | Get one plan | User or admin |
| GET | `/` | Get all plans | User or admin |
| GET | `/operator/{operatorId}` | Get plans for one operator | User or admin |
| GET | `/operator/{operatorId}/active` | Get active plans for one operator | User or admin |
| GET | `/category/{category}` | Get plans by category | User or admin |

Example plan body:

```json
{
  "planName": "Daily 1.5 GB",
  "price": 299,
  "validity": "28 Days",
  "data": "1.5 GB/day",
  "calls": "Unlimited",
  "sms": "100 SMS/day",
  "description": "Popular monthly plan",
  "category": "Unlimited",
  "status": "ACTIVE",
  "operatorId": 1
}
```

## Recharge APIs

Base path:

```text
/api/recharge
```

| Method | Path | Purpose | Common role |
|---|---|---|---|
| POST | `/initiate` | Start a recharge | User |
| PUT | `/{id}/cancel` | Cancel recharge | User or admin |
| GET | `/my-history` | Get logged-in user's recharge history | User |
| GET | `/all` | Get all recharge records | Admin |
| GET | `/status/{status}` | Filter recharge records by status | Admin |
| GET | `/mobile/{mobileNumber}` | Filter recharge records by mobile number | Admin |
| GET | `/{id}` | Get one recharge record | User or admin |
| PUT | `/update-status/{rechargeId}` | Update recharge status | Internal service call |

Example initiate body:

```json
{
  "mobileNumber": "9876543210",
  "operatorId": 1,
  "planId": 1
}
```

## Transaction APIs

Base path:

```text
/api/transactions
```

| Method | Path | Purpose | Common role |
|---|---|---|---|
| POST | `/pay` | Pay for a recharge | User |
| POST | `/wallet/topup` | Add money to wallet | User |
| GET | `/my-transactions` | Get logged-in user's transactions | User |
| GET | `/txn/{transactionId}` | Get one transaction by transaction ID | User or admin |
| GET | `/recharge/{rechargeId}` | Get transaction for recharge | User or admin |
| GET | `/all` | Get all transactions | Admin |
| GET | `/status/{status}` | Filter transactions by status | Admin |
| GET | `/mobile/{mobileNumber}` | Filter transactions by mobile number | Admin |

Example payment body:

```json
{
  "rechargeId": 1,
  "paymentMethod": "UPI",
  "amount": 299,
  "upiId": "user@upi"
}
```

## Swagger URLs

When services are running, Swagger can help you test APIs in the browser.

| Service | Swagger URL |
|---|---|
| API Gateway | http://localhost:8080/swagger-ui.html |
| User Service | http://localhost:8081/swagger-ui.html |
| Operator Service | http://localhost:8082/swagger-ui.html |
| Recharge Service | http://localhost:8083/swagger-ui.html |
| Payment Service | http://localhost:8084/swagger-ui.html |
| Notification Service | http://localhost:8085/swagger-ui.html |
