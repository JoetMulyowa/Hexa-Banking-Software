# Yo! Payments Integration for Loan Repayments

This document describes the implementation of loan repayments using Yo! Payments mobile money service in the Apache Fineract system.

## Overview

The Yo! Payments integration allows Apache Fineract to process loan repayments via the Yo! Payments API, enabling clients to make loan repayments using their mobile money accounts. This integration uses the "acdepositfunds" API method which pulls funds from a customer's mobile money account to the institution's account.

## Components Implemented

1. **YoPaymentRepaymentService** - Core service for processing loan repayments via Yo! Payments API
   - `processRepayment()` - Sends a mobile money repayment request to Yo! Payments
   - `checkRepaymentStatus()` - Checks the status of a pending transaction

2. **YoPaymentRepaymentServiceImpl** - Implementation of the core service
   - Handles communication with Yo! Payments API
   - Manages transaction records

3. **YoPaymentRepaymentIntegrationService** - Interface for integrating with Fineract loan repayment workflow
   - `processRepayment()` - Processes a repayment request
   - `isEnabled()` - Checks if integration is enabled
   - `getPaymentTypeCode()` - Returns the payment type code

4. **YoPaymentRepaymentIntegrationServiceImpl** - Implementation of the integration service
   - Coordinates between Fineract and the Yo! Payments service

5. **MomoPaymentProviderFactory** - Updated to support repayment providers
   - Added repayment provider registration
   - Added method to get appropriate repayment provider

6. **LoanWritePlatformServiceJpaRepositoryImpl** - Modified to handle mobile money repayments
   - Added `integrateRepaymentWithMomoPayment()` method
   - Updated loan repayment workflow to check for Yo! Payments

7. **MomoApiResource** - REST endpoint for checking transaction status
   - `checkYoPaymentTransactionStatus()` - API endpoint for checking payment status

8. **Database Changes**
   - Added payment type for Yo! Payments
   - Added configuration flag to enable/disable the integration

## Configuration

The following configuration properties are required in the `application.properties` file:

```properties
# Yo Payment Integration Configuration
yo.payment.api.url=https://paymentsapi1.yo.co.ug/ybs/task.php
yo.payment.api.username=your_api_username
yo.payment.api.password=your_api_password
yo.payment.provider.code=MPSA
```

## How It Works

1. **Loan Repayment Request**: When a loan repayment is made using the Yo! Payments payment type, the `LoanWritePlatformServiceJpaRepositoryImpl` detects this and calls the `integrateRepaymentWithMomoPayment()` method.

2. **Mobile Money Data**: The system collects the required information:
   - Client's mobile number
   - Repayment amount
   - Transaction details

3. **Yo! Payments Integration**: The `YoPaymentRepaymentIntegrationService` is called to process the repayment.

4. **API Call**: The `YoPaymentRepaymentService` builds an XML request for the `acdepositfunds` API method and sends it to the Yo! Payments API.

5. **Transaction Tracking**: The system creates and updates a `YoPaymentTransaction` record to track the status of the transaction.

6. **Status Checking**: The status of pending transactions can be checked using the `checkRepaymentStatus()` method, which is exposed via the REST API.

## API Endpoints

- `GET /v1/momo/yo/status/{transactionReference}`: Check the status of a Yo! Payment transaction

## Error Handling

- The system logs all API calls and responses for debugging purposes
- Appropriate error messages are returned for various failure scenarios
- Transaction records include error information for failed requests

## Security Considerations

- API credentials are securely stored in the application configuration
- All communication with the Yo! Payments API is encrypted using TLS
- For development/testing environments, the system includes a trust-all certificate validation (should be replaced with proper certificate validation in production)

## Limitations

- The integration requires clients to have valid mobile money accounts
- Clients must have their mobile numbers registered in the system
- The mobile money provider must support the Yo! Payments service 