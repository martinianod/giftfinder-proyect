# Gift Card System API Documentation

## Overview

The Gift Card System allows users to purchase, send, and redeem gift cards within the GiftFinder application. Gift cards can be redeemed into a user's wallet balance, which can be used for future purchases.

## Features

- **Gift Card Creation**: Users can create gift cards for recipients with custom amounts and messages
- **Secure Codes**: Gift cards use secure, unguessable codes with BCrypt hashing
- **Status Lifecycle**: CREATED → SENT → REDEEMED / EXPIRED / CANCELLED
- **Wallet Integration**: Redeemed gift cards credit the user's wallet balance
- **Idempotent Redemption**: Gift cards cannot be redeemed multiple times
- **Expiration Policy**: Default 12 months expiration, configurable
- **Email Notifications**: Recipients receive email notifications with gift card codes
- **Admin Controls**: Admins can cancel or expire gift cards

## API Endpoints

### 1. Create Gift Card

Create a new gift card for a recipient.

**Endpoint:** `POST /api/giftcards`

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "amount": 100.00,
  "currency": "ARS",
  "message": "Happy Birthday! Enjoy shopping!",
  "recipientEmail": "recipient@example.com",
  "deliveryDate": "2024-12-25",
  "expiryMonths": 12
}
```

**Request Fields:**
- `amount` (required): Gift card amount (must be > 0)
- `currency` (required): 3-letter currency code (e.g., "ARS", "USD")
- `message` (optional): Personal message (max 500 characters)
- `recipientEmail` (required): Recipient's email address
- `deliveryDate` (optional): Date to send the gift card (future date). If null or today, sends immediately
- `expiryMonths` (optional): Months until expiration (default: 12, max: 60)

**Response:** `201 Created`
```json
{
  "id": 1,
  "code": "ABC123XYZ456",
  "amount": 100.00,
  "currency": "ARS",
  "message": "Happy Birthday! Enjoy shopping!",
  "recipientEmail": "recipient@example.com",
  "deliveryDate": "2024-12-25",
  "status": "CREATED",
  "expiryDate": "2025-12-25",
  "createdAt": "2024-01-14T10:00:00",
  "redeemedAt": null
}
```

**Validation Errors:** `400 Bad Request`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "amount",
      "message": "Amount must be greater than 0"
    }
  ]
}
```

---

### 2. Redeem Gift Card

Redeem a gift card and credit the amount to the user's wallet.

**Endpoint:** `POST /api/giftcards/redeem`

**Authentication:** Required (JWT)

**Request Body:**
```json
{
  "code": "ABC123XYZ456"
}
```

**Request Fields:**
- `code` (required): The gift card code

**Response:** `200 OK`
```json
{
  "id": 1,
  "code": "ABC123XYZ456",
  "amount": 100.00,
  "currency": "ARS",
  "message": "Happy Birthday! Enjoy shopping!",
  "recipientEmail": "recipient@example.com",
  "deliveryDate": null,
  "status": "REDEEMED",
  "expiryDate": "2025-12-25",
  "createdAt": "2024-01-14T10:00:00",
  "redeemedAt": "2024-01-15T10:00:00"
}
```

**Error Responses:**

Invalid code: `400 Bad Request`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid gift card code"
}
```

Already redeemed: `400 Bad Request`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Gift card has already been redeemed"
}
```

Expired: `400 Bad Request`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Gift card has expired"
}
```

**Notes:**
- Redemption is idempotent - if the ledger entry already exists, no duplicate credit is made
- Gift cards are automatically marked as expired if past their expiry date
- Redeemed amount is credited to the user's wallet
- A ledger entry is created for audit purposes

---

### 3. Get My Gift Cards

Retrieve all gift cards sent by the authenticated user.

**Endpoint:** `GET /api/giftcards`

**Authentication:** Required (JWT)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "code": "ABC123XYZ456",
    "amount": 100.00,
    "currency": "ARS",
    "message": "Happy Birthday!",
    "recipientEmail": "recipient@example.com",
    "deliveryDate": null,
    "status": "REDEEMED",
    "expiryDate": "2025-12-25",
    "createdAt": "2024-01-14T10:00:00",
    "redeemedAt": "2024-01-15T10:00:00"
  },
  {
    "id": 2,
    "code": "DEF456GHI789",
    "amount": 50.00,
    "currency": "ARS",
    "message": "Merry Christmas!",
    "recipientEmail": "another@example.com",
    "deliveryDate": "2024-12-25",
    "status": "SENT",
    "expiryDate": "2025-12-25",
    "createdAt": "2024-01-14T11:00:00",
    "redeemedAt": null
  }
]
```

**Notes:**
- Returns gift cards ordered by creation date (newest first)
- Only returns gift cards sent by the authenticated user
- Does not return gift cards received

---

### 4. Get Wallet

Retrieve the authenticated user's wallet balance and recent transactions.

**Endpoint:** `GET /api/wallet`

**Authentication:** Required (JWT)

**Response:** `200 OK`
```json
{
  "id": 1,
  "balance": 150.00,
  "currency": "ARS",
  "createdAt": "2024-01-14T10:00:00",
  "updatedAt": "2024-01-15T10:00:00",
  "recentTransactions": [
    {
      "id": 1,
      "amount": 100.00,
      "type": "CREDIT",
      "sourceType": "GIFT_CARD_REDEMPTION",
      "referenceId": "ABC123XYZ456",
      "description": "Gift card redemption: Happy Birthday!",
      "createdAt": "2024-01-15T10:00:00"
    },
    {
      "id": 2,
      "amount": 50.00,
      "type": "CREDIT",
      "sourceType": "GIFT_CARD_REDEMPTION",
      "referenceId": "DEF456GHI789",
      "description": "Gift card redemption: Merry Christmas!",
      "createdAt": "2024-01-15T11:00:00"
    }
  ]
}
```

**Notes:**
- Automatically creates a wallet if the user doesn't have one
- Returns all ledger entries ordered by creation date (newest first)
- Balance is the sum of all credits minus debits

---

### 5. Cancel Gift Card (Admin Only)

Cancel a gift card. Only administrators can cancel gift cards.

**Endpoint:** `POST /api/admin/giftcards/{id}/cancel`

**Authentication:** Required (JWT with ADMIN role)

**Path Parameters:**
- `id`: Gift card ID

**Response:** `200 OK`

**Error Responses:**

Not found: `404 Not Found`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Gift card not found"
}
```

Already redeemed: `400 Bad Request`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot cancel a redeemed gift card"
}
```

Unauthorized: `403 Forbidden`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied"
}
```

---

### 6. Expire Gift Card (Admin Only)

Manually expire a gift card. Only administrators can expire gift cards.

**Endpoint:** `POST /api/admin/giftcards/{id}/expire`

**Authentication:** Required (JWT with ADMIN role)

**Path Parameters:**
- `id`: Gift card ID

**Response:** `200 OK`

**Error Responses:**

Not found: `404 Not Found`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Gift card not found"
}
```

Already redeemed: `400 Bad Request`
```json
{
  "timestamp": "2024-01-14T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot expire a redeemed gift card"
}
```

---

## Gift Card Status Lifecycle

```
CREATED
   ↓
   ├─→ SENT (email sent to recipient)
   │     ↓
   │     ├─→ REDEEMED (successfully redeemed)
   │     ├─→ EXPIRED (past expiry date)
   │     └─→ CANCELLED (admin action)
   │
   ├─→ EXPIRED (past expiry date before sending)
   └─→ CANCELLED (admin action)
```

## Wallet Transaction Types

### Transaction Types
- `CREDIT`: Add money to wallet
- `DEBIT`: Remove money from wallet

### Source Types
- `GIFT_CARD_REDEMPTION`: From redeeming a gift card
- `PURCHASE`: From making a purchase
- `REFUND`: From refunding a purchase
- `ADJUSTMENT`: Manual adjustment by admin

## Security Features

### Code Generation
- Codes are 16 characters long
- Generated using UUID with secure random
- Unique across all gift cards
- Unguessable (2^64 combinations)

### Code Storage
- Plain codes stored for user display and email
- BCrypt hashed codes stored for validation
- Hashing prevents code leakage from database breaches

### Idempotency
- Redemption checks if ledger entry already exists
- Prevents double redemption from network retries
- Uses `referenceId` (gift card code) + `sourceType` as idempotency key

### Rate Limiting
- Redemption endpoint should be rate-limited (recommended: 10 requests per minute per user)
- Consider implementing exponential backoff for failed attempts

### Audit Logging
- All gift card creation logged with user ID and recipient
- All redemptions logged with user ID and gift card ID
- All admin actions (cancel, expire) logged
- All wallet operations logged in ledger

## Email Notifications

### Gift Card Email
Recipients receive an email when a gift card is sent containing:
- Sender's name
- Gift card amount and currency
- Personal message (if provided)
- Gift card code
- Expiry date
- Link to redeem

Template location: `src/main/resources/templates/email/gift_card.html`

## Scheduled Jobs

### Gift Card Expiration Job
- **Schedule**: Daily at 3 AM (configurable via `scheduler.gift-card-expiration.cron`)
- **Function**: Processes all gift cards that have passed their expiry date
- **Action**: Updates status from CREATED/SENT to EXPIRED

## Configuration

### Application Properties

```yaml
scheduler:
  gift-card-expiration:
    cron: ${GIFT_CARD_EXPIRATION_JOB_CRON:0 0 3 * * *}  # Daily at 3 AM

app:
  base-url: ${APP_BASE_URL:http://localhost:5173}  # Used in email templates
```

### Environment Variables
- `GIFT_CARD_EXPIRATION_JOB_CRON`: Cron expression for expiration job (default: `0 0 3 * * *`)
- `APP_BASE_URL`: Base URL of the frontend application for email links

## Database Schema

### gift_cards table
```sql
CREATE TABLE gift_cards (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    hashed_code VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    message VARCHAR(500),
    sender_user_id BIGINT NOT NULL REFERENCES users(id),
    recipient_email VARCHAR(255) NOT NULL,
    delivery_date DATE,
    status VARCHAR(50) NOT NULL,
    expiry_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    redeemed_at TIMESTAMP,
    redeemed_by_user_id BIGINT REFERENCES users(id)
);
```

### wallets table
```sql
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
    balance DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT
);
```

### wallet_ledger_entries table
```sql
CREATE TABLE wallet_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    amount DECIMAL(19,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_wallet_id ON wallet_ledger_entries(wallet_id);
CREATE INDEX idx_reference_id ON wallet_ledger_entries(reference_id);
```

## Example Usage

### 1. Create and send a gift card immediately

```bash
curl -X POST http://localhost:8080/api/giftcards \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "currency": "ARS",
    "message": "Happy Birthday!",
    "recipientEmail": "friend@example.com"
  }'
```

### 2. Create a gift card for future delivery

```bash
curl -X POST http://localhost:8080/api/giftcards \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150.00,
    "currency": "ARS",
    "message": "Merry Christmas!",
    "recipientEmail": "friend@example.com",
    "deliveryDate": "2024-12-25"
  }'
```

### 3. Redeem a gift card

```bash
curl -X POST http://localhost:8080/api/giftcards/redeem \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ABC123XYZ456"
  }'
```

### 4. Check wallet balance

```bash
curl -X GET http://localhost:8080/api/wallet \
  -H "Authorization: Bearer <token>"
```

### 5. View sent gift cards

```bash
curl -X GET http://localhost:8080/api/giftcards \
  -H "Authorization: Bearer <token>"
```

## Best Practices

### For Users
1. **Keep codes secure**: Gift card codes should be treated like cash
2. **Redeem promptly**: Redeem gift cards before they expire
3. **Check expiry dates**: Gift cards expire after 12 months by default
4. **Verify amounts**: Check the amount before redeeming

### For Developers
1. **Never log codes**: Don't log gift card codes in plain text
2. **Use HTTPS**: Always use HTTPS in production for API calls
3. **Implement rate limiting**: Prevent brute force attacks on redemption
4. **Monitor redemptions**: Set up alerts for suspicious redemption patterns
5. **Regular cleanup**: Ensure expired gift cards are processed regularly
6. **Backup ledger**: Wallet ledger is critical for accounting - ensure regular backups

### For Administrators
1. **Monitor unclaimed cards**: Track gift cards that haven't been redeemed
2. **Handle disputes**: Use admin endpoints to cancel/expire cards when needed
3. **Audit regularly**: Review ledger entries for anomalies
4. **Set appropriate limits**: Consider setting maximum gift card amounts
5. **Customer support**: Be prepared to help users with lost or expired codes

## Troubleshooting

### Gift card not received
1. Check if email was sent (check notification logs)
2. Verify recipient email address
3. Check spam/junk folders
4. Resend if needed (contact admin)

### Cannot redeem gift card
1. Verify code is correct (case-sensitive)
2. Check if already redeemed
3. Verify gift card hasn't expired
4. Ensure gift card hasn't been cancelled

### Wallet balance incorrect
1. Review ledger entries for the wallet
2. Verify all transactions are accounted for
3. Check for failed redemptions
4. Contact admin for manual adjustment if needed

## Future Enhancements

Potential future improvements:
- Partial redemption support
- Gift card transfer between users
- Bulk gift card creation
- Gift card templates
- Customizable expiry periods per card
- Gift card analytics and reporting
- Gift card splitting (split one card into multiple)
- Auto-apply wallet balance at checkout
- Gift card gifting suggestions based on user preferences
