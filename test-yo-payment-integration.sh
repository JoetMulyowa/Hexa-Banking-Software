#!/bin/bash

# Test script for Yo Payment integration
echo "Testing Yo Payment Integration"

# Set environment variables
BASE_URL="https://localhost:8443/fineract-provider/api/v1"
TENANT_ID="default"
CONTENT_TYPE="application/json"
USERNAME="mifos"
PASSWORD="password"

# 1. Get authentication token
echo "Getting authentication token..."
TOKEN=$(curl -k -s -X POST "$BASE_URL/authentication" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: $TENANT_ID" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" | grep -o '"base64EncodedAuthenticationKey":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "Failed to get token!"
  exit 1
fi

echo "Got token: $TOKEN"

# 2. Enable Yo Payment Mobile Money payment in global configuration
echo "Enabling Yo Payment Mobile Money..."
curl -k -X PUT "$BASE_URL/configurations/enable-yo-payment-mobile-money-payment" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: $TENANT_ID" \
  -H "Authorization: Basic $TOKEN" \
  -d '{"enabled": true}'

# 3. Create a test client (if needed)
CLIENT_ID=1  # Replace with actual client ID if needed

# 4. Create a test loan product (if needed)
LOAN_PRODUCT_ID=1  # Replace with actual loan product ID if needed

# 5. Create a loan application
echo "Creating loan application..."
LOAN_RESPONSE=$(curl -k -s -X POST "$BASE_URL/loans" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: $TENANT_ID" \
  -H "Authorization: Basic $TOKEN" \
  -d '{
    "clientId": '"$CLIENT_ID"',
    "productId": '"$LOAN_PRODUCT_ID"',
    "principal": "10000",
    "loanTermFrequency": 12,
    "loanTermFrequencyType": 2,
    "loanType": "individual",
    "numberOfRepayments": 12,
    "repaymentEvery": 1,
    "repaymentFrequencyType": 2,
    "interestRatePerPeriod": 10,
    "amortizationType": 1,
    "interestType": 0,
    "interestCalculationPeriodType": 1,
    "transactionProcessingStrategyId": 1,
    "expectedDisbursementDate": "01 January 2023",
    "submittedOnDate": "01 January 2023"
  }')

LOAN_ID=$(echo $LOAN_RESPONSE | grep -o '"loanId":[0-9]*' | cut -d: -f2)
echo "Created loan with ID: $LOAN_ID"

# 6. Approve loan
echo "Approving loan..."
curl -k -X POST "$BASE_URL/loans/$LOAN_ID?command=approve" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: $TENANT_ID" \
  -H "Authorization: Basic $TOKEN" \
  -d '{
    "approvedOnDate": "01 January 2023",
    "approvedLoanAmount": "10000",
    "note": "Loan approved"
  }'

# 7. Disburse loan with Yo Payment
echo "Disbursing loan with Yo Payment..."
curl -k -X POST "$BASE_URL/loans/$LOAN_ID?command=disburse" \
  -H "Content-Type: application/json" \
  -H "Fineract-Platform-TenantId: $TENANT_ID" \
  -H "Authorization: Basic $TOKEN" \
  -d '{
    "actualDisbursementDate": "01 January 2023",
    "transactionAmount": "10000",
    "note": "Loan disbursed",
    "paymentTypeId": "101"
  }'

echo "Test completed!" 