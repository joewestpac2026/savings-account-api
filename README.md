# Savings Account API

A small Spring Boot service for creating and retrieving savings accounts.

## Overview

This service exposes two endpoints:

- `POST /api/v1/accounts` to create a savings account
- `GET /api/v1/accounts/{id}` to retrieve a savings account by id

The current implementation also includes:

- request validation
- business rule validation
- idempotent account creation
- audit persistence for both success and failure flows
- masked operational logging
- cached account retrieval
- database failure handling
- API contract design
- PostgreSQL-backed integration testing

## API Endpoints

### Create Savings Account

`POST /api/v1/accounts`

Main request fields:

- `customerId` - customer identifier used to distinguish customers even when names may be the same, and to count how many accounts belong to that customer
- `customerName` - customer name stored with the account
- `accountNickName` - optional account display name, checked against offensive language rules
- `accountType` - 2-character account type code, currently limited to `01` (standard savings), `02` (online savings), `03` (notice savings), and `04` (term savings)
- `channelCode` - 2-character request channel code, currently limited to `01` (branch), `02` (online), and `03` (mobile)
- `branchCode` - 4-digit branch identifier used for validation and included in the generated account number; this implementation assumes valid values in the range `0001` to `1999`, based on Westpac-style branch numbering conventions
- `currency` - 3-letter currency code; savings accounts are assumed to support multiple currencies
- `transactionReference` - request reference supplied by the calling system, used for tracing and idempotency

Example request:

```json
{
  "customerId": "123456789",
  "customerName": "Joe Wu",
  "accountNickName": "Payments engineer",
  "accountType": "01",
  "channelCode": "02",
  "branchCode": "0278",
  "currency": "NZD",
  "transactionReference": "TXN-1001"
} 
```
If the request succeeds, the response returns the saved account data together with generated values such as `id`, `accountNumber`, and `createdAt`. Newly created accounts currently return `status` as `ACTIVE`, based on the assumption that account status may change later in the account lifecycle.

Example success response:

```json
{
  "responseCode": "0000",
  "responseDescription": "Success",
  "id": 1,
  "customerId": "123456789",
  "accountNumber": "03-0278-0000001-000",
  "accountType": "01",
  "customerName": "Joe Wu",
  "accountNickName": "Payments engineer",
  "channelCode": "02",
  "branchCode": "0278",
  "currency": "NZD",
  "transactionReference": "TXN-1001",
  "createdAt": "2026-04-22T15:00:00Z",
  "status": "ACTIVE"
}
```
If the request fails, the service returns a consistent error response containing `responseCode`, `responseDescription`, and the original `transactionReference`.

Example error response:

```json
{
  "responseCode": "1004",
  "responseDescription": "branchCode must be between 0001 and 1999",
  "transactionReference": "TXN-1001"
}
```

### Get Savings Account By Id

`GET /api/v1/accounts/{id}`

Example:

```http
GET /api/v1/accounts/1
```
If the account exists, the service returns the saved account details, including customer information, generated account number, branch code, currency, creation timestamp, and current status.
If the account does not exist, the service returns a not found response.

Example not found response:

```json
{
  "responseCode": "2001",
  "responseDescription": "Savings account not found for id=: 1"
}
```
If the supplied id is not numeric, the service returns a 400 Bad Request response.

Example invalid id response:

```json
{
"responseCode": "1009",
"responseDescription": "id must be a numeric value"
}
```

## Request Validation

The request is validated in two stages.

The first stage checks the basic request shape, such as required fields and field length constraints. More business-specific checks, such as branch ranges, supported account types, and customer account limits, are handled separately in the service layer.

### Required fields

- `customerId`
- `customerName`
- `accountType`
- `channelCode`
- `branchCode`
- `currency`
- `transactionReference`

`accountNickName` is optional.

### Field constraints

- `customerId` must be exactly 9 characters
- `customerName` must be at most 100 characters
- `accountNickName` must be between 5 and 30 characters when provided
- `accountType` must be exactly 2 characters
- `channelCode` must be exactly 2 characters
- `branchCode` must be exactly 4 characters
- `currency` must be exactly 3 characters
- `transactionReference` must be at most 50 characters

## Business Rules

After the basic request validation passes, the service applies additional business rules before an account is created.

- A customer cannot hold more than 5 savings accounts in this service
- `branchCode` must be numeric and fall within the supported range of `0001` to `1999`
- `channelCode` must be one of the supported request channels: `01`, `02`, or `03`
- `accountType` must be one of the supported account type codes: `01`, `02`, `03`, or `04`
- `accountNickName`, when provided, must not contain offensive language
- Repeated create requests with the same `transactionReference` and `customerId` are treated as idempotent and return the existing account instead of creating a new one

## Assumptions and Design Decisions

This project includes a few assumptions beyond the bare minimum endpoint contract so the service behaves more like a small banking-style account opening flow.

### Request and account assumptions

Some request fields were added deliberately to make the API contract more explicit and realistic.

- `customerId` is treated as the main customer identifier, because customer names are not assumed to be unique
- `branchCode` is validated and also used as part of the generated account number
- `channelCode` is used to represent where the request originated
- `accountType` is restricted to a small set of supported account type codes defined by this implementation
- `currency` is included based on the assumption that savings accounts may be opened in multiple currencies
- `transactionReference` is supplied by the calling system for request tracing and idempotency

These fields were added after looking into how account opening is commonly constrained and traced in banking-style systems.

### Error handling design

Validation failures and business rule failures are intentionally returned with domain response codes instead of raw framework exception output.

Some failures are also split into separate response codes where the cause is meaningfully different. For example, invalid `branchCode` format and out-of-range `branchCode` are handled as separate cases.

This keeps failures easier to diagnose and test.

### Audit and logging design

Account creation writes request and response data to an audit table so both successful and failed creation attempts can be traced later.

Operational logging is handled separately from audit persistence. The log file keeps customer and account information masked, while the audit table keeps the full request and response payload needed for internal traceability.

### Idempotency

The service treats `transactionReference + customerId` as the idempotency key for account creation.

If the same request is submitted again with the same combination, the service returns the existing account instead of creating a duplicate row.

### Testing design

The automated test strategy is intentionally centred on a PostgreSQL-backed integration test.

This was chosen to verify the full flow together, including validation, business rules, persistence, idempotency, audit updates, and failure handling, instead of splitting the coverage across several smaller mocked test classes.

### Manual testing

In addition to the automated integration test, the API was also exercised manually using `requests.http`.

Manual checks were used to verify:

- successful account creation
- repeated requests with the same `transactionReference`
- validation failures
- business rule failures
- database persistence
- audit table updates
- masked operational log output

## Error Codes

The API uses application-level response codes in addition to HTTP status codes.

In general:

- `0000` is used for successful responses
- `1xxx` is used for validation and business rule failures
- `2xxx` is used for lookup failures
- `3xxx` is used for database-related failures

Current response codes used by this service:

- `0000` - success
- `1000` - request validation failed
- `1001` - customer already has 5 accounts
- `1002` - offensive nickname detected
- `1003` - `branchCode` is not a 4-digit number
- `1004` - `branchCode` is outside the allowed range
- `1006` - invalid `channelCode`
- `1008` - invalid `accountType`
- `1009` - id must be a numeric value
- `2001` - savings account not found
- `3001` - database connection is unavailable
- `3002` - database is temporarily unavailable

## Account Number Format

Generated account numbers follow this format:

```text
03-BBBB-AAAAAAA-000
```

Where:

- `03` is the Westpac bank code used in this implementation
- `BBBB` is the branch code
- `AAAAAAA` is the generated account sequence
- `000` is the suffix

Example:

```text
03-0278-0000001-000
```
The generated account number is intended to follow a bank-style structure rather than use a random identifier.

## Running the Application

### Prerequisites

Make sure the following are installed:

- Java 21
- PostgreSQL
- Maven or the included Maven wrapper

### Database Setup

Create a PostgreSQL database named: 

```text
savings_db
```

### Environment Variables

The main application reads database settings from environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

When the application starts, it will create and update the required tables automatically through JPA. The main tables used by this service are:

- `savings_accounts`
- `account_creation_audits`
- `offensive_nicknames`

## Running Tests

The automated tests use a real PostgreSQL database.

### Automated tests

Main test class:

`src/test/java/com/assignment/savings/account/api/SavingsAccountIntegrationTest.java`

Test configuration:

`src/test/resources/application-test.yml`

### Test Coverage

The integration test suite covers:

- successful account creation
- idempotent repeated requests
- offensive nickname rejection
- customer account limit rejection
- invalid request body validation
- invalid branch code rejection
- audit persistence verification

### Manual tests

Manual request scenarios are collected in:

`requests.http`

The manual scenarios currently cover:

- create account - success
- create account - same customer name, different customerId
- create account - same customer, different branch
- create account - different customer, same branch
- create account - idempotent request, first submission
- create account - idempotent request, repeated identical submission
- create account - invalid nickname
- create account - invalid request body, required field blank
- create account - invalid request body, accountType wrong length
- create account - invalid branchCode range
- create account - invalid branchCode format
- create account - customer account limit, first account
- create account - customer account limit, second account
- create account - customer account limit, third account
- create account - customer account limit, fourth account
- create account - customer account limit, fifth account
- create account - customer account limit, sixth account should fail
- get savings account by id - success
- get savings account by id - not found
- get savings account by id - invalid id type
- get savings account by id - empty id path

## Logging and Audit

### Masked Operational Log

Masked operational logs are written to:

```text
logs/account-creation-audit.log
```

The dedicated logger is configured in:

`src/main/resources/logback-spring.xml`

### Audit Record Persistence

For account creation requests, the service stores audit data including:

- transaction reference
- channel code
- request payload
- response payload
- HTTP status
- response code
- response description
- request and response timestamps

This supports traceability for both success and failure paths.

## Project Structure

- `src/main/java` - application source code, including API, service, domain, exception handling, and logging support
- `src/main/resources` - application and logging configuration
- `src/test/java` - PostgreSQL-backed integration tests
- `src/test/resources` - test profile configuration
- `requests.http` - manual request scenarios for local verification

## Future Improvements

- add a dedicated lookup integration test suite for the `GET /api/v1/accounts/{id}` endpoint
- externalise reference data such as account types and channel codes instead of keeping them hard-coded in service validation
- introduce a more explicit account lifecycle if additional account states beyond `ACTIVE` are needed
- replace the current simple cache with Redis if the service needs shared or distributed caching later