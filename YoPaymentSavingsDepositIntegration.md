# YoPayment Savings Deposit Integration

This document describes the integration between Apache Fineract and Yo Payments for processing mobile money deposits into savings accounts.

## Overview

The YoPayment Savings Deposit integration allows clients to deposit funds into their savings accounts directly from their mobile money wallets. The integration uses Yo Payments' API to process mobile money transactions and updates the Fineract system accordingly.

## Configuration

The following configuration properties need to be set in the application properties:

```
# YoPayment API Configuration
yo.payment.api.url=https://paymentsapi1.yo.co.ug/ybs/task.php
yo.payment.api.username=your_api_username
yo.payment.api.password=your_api_password
yo.payment.provider.code=MPSA
```

Additionally, you need to enable the integration by updating the global configuration:

```sql
UPDATE c_configuration SET enabled = 1 WHERE name = 'enable-yo-payment-savings-deposit';
```

## API Endpoints

### Initiate Deposit

**Endpoint:** `POST /v1/savings/{savingsId}/momo/deposit`

**Request Body:**
```json
{
  "transactionAmount": 100.00,
  "mobileNumber": "256771234567",
  "note": "Deposit via mobile money",
  "transactionDate": "2023-05-20"
}
```

**Required Fields:**
- `transactionAmount`: The amount to deposit (decimal)
- `mobileNumber`: The mobile number to debit (string)

**Optional Fields:**
- `note`: A note for the transaction (string)
- `transactionDate`: The transaction date (date, defaults to current date)

**Response:**
```json
{
  "savingsId": 123,
  "resourceId": 456,
  "status": "PENDING"
}
```

The `status` field will be either:
- `SUCCESS`: If the transaction was processed immediately
- `PENDING`: If the transaction is pending and will be processed asynchronously

## Database Tables

The integration uses the following table to track mobile money deposit transactions:

```sql
CREATE TABLE m_yo_payment_savings_deposit_transaction (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  savings_account_id BIGINT NOT NULL,
  savings_transaction_id BIGINT,
  vendor_tran_id VARCHAR(100),
  middleware_reference_no VARCHAR(100),
  transaction_date DATETIME NOT NULL,
  amount DECIMAL(19,6) NOT NULL,
  is_reversed BOOLEAN NOT NULL DEFAULT FALSE,
  status_code VARCHAR(10),
  status_desc VARCHAR(100),
  mobile_number VARCHAR(20) NOT NULL,
  request_body TEXT,
  response_body TEXT,
  error_msg VARCHAR(500),
  createdby_id BIGINT NOT NULL,
  created_date DATETIME NOT NULL,
  lastmodifiedby_id BIGINT,
  lastmodified_date DATETIME,
  FOREIGN KEY (savings_account_id) REFERENCES m_savings_account(id),
  FOREIGN KEY (savings_transaction_id) REFERENCES m_savings_account_transaction(id)
);
```

## Transaction Flow

1. Client initiates a deposit via the API
2. Fineract creates a record in the `m_yo_payment_savings_deposit_transaction` table
3. Fineract sends a deposit request to Yo Payments API
4. If the transaction is successful immediately, Fineract creates a deposit transaction in the savings account
5. If the transaction is pending, Fineract will check the status later and complete the transaction when successful

## Error Handling

The integration handles various error scenarios:
- API communication errors
- Invalid mobile number or amount
- Insufficient funds in mobile money wallet
- Failed transactions

All errors are logged and stored in the transaction record for troubleshooting.

## Implementation Classes

The integration consists of the following main components:

1. **Domain Entity**: `YoPaymentSavingsDepositTransaction` - Represents a mobile money deposit transaction
2. **Repository**: `YoPaymentSavingsDepositTransactionRepository` - Data access for transaction records
3. **Service Interface**: `YoPaymentSavingsDepositService` - Core service for processing deposits
4. **Service Implementation**: `YoPaymentSavingsDepositServiceImpl` - Implementation of the service
5. **Integration Interface**: `YoPaymentSavingsDepositIntegrationService` - High-level integration service
6. **Integration Implementation**: `YoPaymentSavingsDepositIntegrationServiceImpl` - Implementation of the integration
7. **API Resource**: `SavingsMomoApiResource` - REST API endpoint for initiating deposits 