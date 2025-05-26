# YoPayment Mobile Money Savings Deposit Integration

This integration enables Apache Fineract to process savings account deposits via Yo Payments mobile money service. It allows clients to deposit funds into their savings accounts directly from their mobile money wallets.

## Features

- Process mobile money deposits to savings accounts
- Track transaction status and history
- Handle successful and failed transactions
- Support for asynchronous transaction processing

## Setup Instructions

### 1. Database Setup

The integration requires a new database table to track transactions. The migration script is included in the codebase:

- Migration script: `fineract-provider/src/main/resources/db/changelog/tenant/parts/0170_YoPayment-savings-deposit-transaction-table.xml`

This script will be automatically applied during database migrations.

### 2. Configuration

Add the following properties to your `application.properties` file:

```properties
# YoPayment API Configuration
yo.payment.api.url=https://paymentsapi1.yo.co.ug/ybs/task.php
yo.payment.api.username=your_api_username
yo.payment.api.password=your_api_password
yo.payment.provider.code=MPSA
```

Replace the values with your actual Yo Payments API credentials.

### 3. Enable the Integration

By default, the integration is disabled. To enable it:

```sql
UPDATE c_configuration SET enabled = 1 WHERE name = 'enable-yo-payment-savings-deposit';
```

## API Usage

### Initiating a Deposit

To initiate a mobile money deposit:

```
POST /v1/savings/{savingsId}/momo/deposit
```

Request body:
```json
{
  "transactionAmount": 100.00,
  "mobileNumber": "256771234567",
  "note": "Deposit via mobile money",
  "transactionDate": "2023-05-20"
}
```

Response:
```json
{
  "savingsId": 123,
  "resourceId": 456,
  "status": "PENDING"
}
```

## Implementation Details

The integration consists of the following components:

1. **Domain Entity**: `YoPaymentSavingsDepositTransaction` - For tracking deposit transactions
2. **Repository**: `YoPaymentSavingsDepositTransactionRepository` - Data access layer
3. **Service Interface**: `YoPaymentSavingsDepositService` - Core service for processing deposits
4. **Service Implementation**: `YoPaymentSavingsDepositServiceImpl` - Implementation of the service
5. **Integration Interface**: `YoPaymentSavingsDepositIntegrationService` - High-level integration service
6. **Integration Implementation**: `YoPaymentSavingsDepositIntegrationServiceImpl` - Implementation of the integration
7. **API Resource**: `SavingsMomoApiResource` - REST API endpoint for initiating deposits

## Transaction Flow

1. Client initiates a deposit via the API
2. System creates a transaction record in the database
3. System sends a deposit request to Yo Payments API
4. If successful immediately, system creates a deposit transaction in the savings account
5. If pending, system will check the status later and complete when successful

## Error Handling

The integration handles various error scenarios:

- API communication errors
- Invalid mobile number or amount
- Insufficient funds in mobile money wallet
- Failed transactions

All errors are logged and stored in the transaction record for troubleshooting.

## Security Considerations

- API credentials are stored securely in application properties
- All communication with Yo Payments API is encrypted using TLS
- Input validation is performed on all API requests

## Limitations

- The integration requires clients to have valid mobile money accounts
- The mobile money provider must be supported by Yo Payments
- Network connectivity is required for real-time processing

## Testing

For testing in development environments, Yo! Payments provides a sandbox environment:
```
yo.payment.api.url=https://sandbox.yo.co.ug/services/yopaymentsdev/task.php
```

You'll need to register for sandbox credentials with Yo! Payments. 