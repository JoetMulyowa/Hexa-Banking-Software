# Africa's Talking SMS Integration for Apache Fineract

This document provides instructions for integrating Africa's Talking SMS API with Apache Fineract for sending SMS notifications to clients.

## Overview

Apache Fineract supports sending SMS notifications to clients at various stages of the loan lifecycle, including:
- Loan Submission
- Loan Approval
- Loan Disbursement
- Loan Repayment
- Loan Rejection
- Loan Closure

The Africa's Talking integration allows these notifications to be sent using the Africa's Talking Bulk SMS API.

## Configuration

To configure the Africa's Talking SMS integration, you'll need to update the following properties in your `application.properties` file:

### Select the SMS Provider

```properties
# Set to 'africastalking' to use Africa's Talking, or 'default' for the default SMS provider
fineract.sms.provider=africastalking
```

### Africa's Talking API Configuration

```properties
# Africa's Talking username (required)
africastalking.username=your-username

# Africa's Talking API key (required)
africastalking.api.key=your-api-key

# Sender ID to be displayed to recipients (optional)
africastalking.sender.id=FINERACT

# Set to true to use the sandbox environment, false for production
africastalking.sandbox=false
```

## Environment Variables

You can also configure the integration using environment variables:

```
FINERACT_SMS_PROVIDER=africastalking
FINERACT_AFRICASTALKING_USERNAME=your-username
FINERACT_AFRICASTALKING_API_KEY=your-api-key
FINERACT_AFRICASTALKING_SENDER_ID=FINERACT
FINERACT_AFRICASTALKING_SANDBOX=false
```

## Testing the Integration

1. Ensure that SMS notifications are enabled in Fineract:
   - In the Admin portal, go to "System" > "Global Configurations"
   - Make sure "Enable SMS Notifications" is enabled

2. Set up your Africa's Talking account:
   - Register at [Africa's Talking](https://africastalking.com/)
   - Create an API key
   - Configure your SMS service

3. Trigger SMS notifications:
   - Create a loan application
   - Move it through the various stages (approval, disbursement, etc.)
   - Check that SMS notifications are being sent at each stage

## API Response

The Africa's Talking API will return a response in this format:

```json
{
  "SMSMessageData": {
    "Message": "Sent to 1/1 Total Cost: USD 0.0000",
    "Recipients": [
      {
        "statusCode": 101,
        "number": "+254711XXXYYY",
        "cost": "KES 0.8000",
        "status": "Success",
        "messageId": "ATXid_123456789"
      }
    ]
  }
}
```

## Status Codes

- 100: Processed
- 101: Sent
- 102: Queued
- 401: RiskHold
- 402: InvalidSenderId
- 403: InvalidPhoneNumber
- 404: UnsupportedNumberType
- 405: InsufficientBalance
- 406: UserInBlacklist
- 407: CouldNotRoute
- 409: DoNotDisturbRejection
- 500: InternalServerError
- 501: GatewayError
- 502: RejectedByGateway

## Troubleshooting

- Check the application logs for errors related to SMS sending
- Verify that the Africa's Talking API credentials are correct
- Ensure that the phone numbers are in the correct format (e.g., +2547XXXXXXXX)
- Check your Africa's Talking dashboard for message delivery status
- If using the sandbox environment, remember that messages are not actually sent to real phone numbers

## References

- [Africa's Talking SMS API Documentation](https://developers.africastalking.com/docs/sms/overview)
- [Apache Fineract Documentation](https://fineract.apache.org/) 