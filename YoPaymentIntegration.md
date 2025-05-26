# Yo! Payments Integration with Apache Fineract

This document describes the integration between Apache Fineract and Yo! Payments mobile money service for loan disbursement.

## Overview

The Yo! Payments integration allows Apache Fineract to disburse loans directly to clients' mobile money accounts in Uganda. When a loan is approved and ready for disbursement, the system can automatically send the funds to the client's mobile money account through the Yo! Payments API.

## Prerequisites

To use this integration, you need:

1. A Yo! Payments account with valid API credentials
2. Client mobile numbers stored in the system
3. Configuration settings properly defined
4. The integration feature enabled in Fineract

## Configuration Settings

The following configuration settings are required in the `application.properties` file:

```properties
# URL for Yo Payment API
# Production URLs:
# - Primary: https://paymentsapi1.yo.co.ug/ybs/task.php
# - Secondary: https://paymentsapi2.yo.co.ug/ybs/task.php
# - Sandbox: https://sandbox.yo.co.ug/services/yopaymentsdev/task.php
yo.payment.api.url=${FINERACT_YO_PAYMENT_API_URL:https://sandbox.yo.co.ug/services/yopaymentsdev/task.php}

# API Username and Password
yo.payment.api.username=${FINERACT_YO_PAYMENT_API_USERNAME:your_api_username}
yo.payment.api.password=${FINERACT_YO_PAYMENT_API_PASSWORD:your_api_password}

# Account Provider Code
yo.payment.provider.code=${FINERACT_YO_PAYMENT_PROVIDER_CODE:MTN_UGANDA}

# Additional settings for repayments
yo.payment.notification.endpoint=${FINERACT_YO_PAYMENT_NOTIFICATION_ENDPOINT:/api/v1/yo-payments/webhook}
yo.payment.notification.secret=${FINERACT_YO_PAYMENT_NOTIFICATION_SECRET:your_webhook_secret}
```

## Enabling the Integration

The integration must be enabled in the Fineract global configuration settings. This can be done through the API:

```
PUT https://your-fineract-instance/fineract-provider/api/v1/configurations/enable-yo-payment-mobile-money-payment
Content-Type: application/json
Fineract-Platform-TenantId: default
Authorization: Basic base64_encoded_credentials

{
  "enabled": true
}
```

## Database Schema

The integration creates two dedicated tables for Yo! Payment transactions:

```sql
-- Yo! Payment transactions table (for disbursements)
CREATE TABLE IF NOT EXISTS `m_yo_payment_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `loan_id` BIGINT NOT NULL,
  `loan_transaction_id` BIGINT NULL,
  `vendor_tran_id` VARCHAR(100) NULL,
  `middleware_reference_no` VARCHAR(100) NULL,
  `transaction_date` DATE NOT NULL,
  `amount` DECIMAL(19,6) NOT NULL,
  `is_reversed` TINYINT NOT NULL DEFAULT 0,
  `status_code` VARCHAR(10) NULL,
  `status_desc` VARCHAR(100) NULL,
  `request_body` TEXT NULL,
  `response_body` TEXT NULL,
  `error_msg` VARCHAR(500) NULL,
  `created_by` BIGINT NOT NULL,
  `created_on_utc` DATETIME NOT NULL,
  `last_modified_by` BIGINT NULL,
  `last_modified_on_utc` DATETIME NULL,
  PRIMARY KEY (`id`)
);

-- Yo! Payment repayments table
CREATE TABLE IF NOT EXISTS `m_yo_payment_repayment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `loan_id` BIGINT NOT NULL,
  `loan_transaction_id` BIGINT NULL,
  `vendor_tran_id` VARCHAR(100) NOT NULL,
  `transaction_date` DATETIME NOT NULL,
  `amount` DECIMAL(19,6) NOT NULL,
  `mobile_number` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `processed` TINYINT NOT NULL DEFAULT 0,
  `notification_payload` TEXT NULL,
  `created_on_utc` DATETIME NOT NULL,
  `processed_on_utc` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vendor_tran_id` (`vendor_tran_id`)
);
```

## Integration Flow

1. **Loan Approval**: The loan is first created and then approved through the standard Fineract workflow.

2. **Disbursement Request**: When a loan is ready to be disbursed, a disbursement request is made specifying the `paymentTypeId` that corresponds to Yo! Payments (`YO_PAYMENT_MOMO_PAYMENT`).

3. **Yo! Payment Processing**:
   - The system verifies that Yo! Payment integration is enabled
   - Client's mobile number is extracted from the loan data
   - A Yo! Payment request is created in XML format
   - The request is sent to the Yo! Payments API
   - The response is processed and stored in the database

4. **Transaction Status**: The transaction status (PENDING, SUCCEEDED, FAILED) is recorded in the database and the loan is marked as disbursed via mobile money if the transaction is successful.

## API Request and Response Format

### Request Format

The Yo! Payments API uses XML format. A typical request looks like:

```xml
<AutoCreate>
  <Request>
    <APIUsername>your_api_username</APIUsername>
    <APIPassword>your_api_password</APIPassword>
    <Method>acwithdrawfunds</Method>
    <NonBlocking>FALSE</NonBlocking>
    <Amount>10000</Amount>
    <Account>256771234567</Account>
    <AccountProviderCode>MTN_UGANDA</AccountProviderCode>
    <Narrative>Loan Disbursement</Narrative>
    <ExternalReference>loan_123_456</ExternalReference>
  </Request>
</AutoCreate>
```

### Response Format

A successful response looks like:

```xml
<AutoCreate>
  <Response>
    <Status>OK</Status>
    <StatusCode>100</StatusCode>
    <TransactionStatus>PENDING</TransactionStatus>
    <TransactionReference>yo_ref_123456789</TransactionReference>
  </Response>
</AutoCreate>
```

## Error Handling

The integration handles various error scenarios:

1. **API Connection Errors**: If the connection to the Yo! Payments API fails
2. **Authentication Errors**: If the API credentials are invalid
3. **Validation Errors**: If required fields like the client's mobile number are missing
4. **Transaction Errors**: If the payment transaction fails at the Yo! Payments end

All errors are logged and stored in the database for audit purposes.

## Testing

A test script (`test-yo-payment-integration.sh`) is provided to verify the integration:

1. It authenticates with Fineract
2. Enables the Yo! Payment integration
3. Creates a loan
4. Approves the loan
5. Attempts to disburse the loan through Yo! Payments

For testing purposes, a mock API endpoint is also available at `/mock-yo-payment` that simulates the behavior of the real Yo! Payments API.

## Limitations

1. The integration currently only supports the Ugandan Shilling (UGX) currency
2. Disbursements must be made on the current date or future dates, not in the past
3. The client must have a valid mobile money account with the specified provider

## Troubleshooting

Common issues and their solutions:

1. **Transaction Failed**: Verify the client's mobile number is correctly formatted (should include country code, e.g., 256771234567)
2. **API Connection Issues**: Check network connectivity and API URL configuration
3. **Authentication Errors**: Verify API credentials are correctly set in the configuration
4. **Provider Not Supported**: Ensure the provider code is correctly configured (MTN_UGANDA, AIRTEL_UGANDA, etc.) 