# Proactive Automation Features - GiftFinder AI

This document describes the proactive automation features added to GiftFinder AI, including event reminders and price drop tracking.

## Overview

The proactive automation module enables users to:
1. Store recipients with important dates (birthdays, anniversaries, etc.)
2. Receive automated email reminders before important dates
3. Save products and track their prices
4. Get notified when prices drop below a configured threshold

## Architecture

### Database Schema

#### Recipients
- **Table**: `recipients`
- **Purpose**: Store gift recipients per user
- **Fields**: id, user_id, name, description, birthday, created_at, updated_at

#### Important Dates
- **Table**: `important_dates`
- **Purpose**: Store important dates with reminders
- **Fields**: id, user_id, recipient_id, name, type (BIRTHDAY/HOLIDAY/ANNIVERSARY/CUSTOM), date, recurring, description, created_at, updated_at

#### Reminders
- **Table**: `reminders`
- **Purpose**: Scheduled reminder instances
- **Fields**: id, user_id, important_date_id, scheduled_date, days_before, status (PENDING/SENT/FAILED/CANCELLED), channel (EMAIL/PUSH/WHATSAPP), sent_at, created_at, updated_at
- **Indexes**: idx_reminder_user_scheduled_date, idx_reminder_status_scheduled_date

#### Saved Products
- **Table**: `saved_products`
- **Purpose**: Products saved for price tracking
- **Fields**: id, user_id, recipient_id, product_id, title, current_price, currency, product_url, image_url, store, price_tracking_enabled, price_drop_threshold_percent, created_at, updated_at
- **Indexes**: idx_saved_product_user, idx_saved_product_tracking

#### Price History
- **Table**: `price_history`
- **Purpose**: Track price changes over time
- **Fields**: id, saved_product_id, price, currency, checked_at, available
- **Indexes**: idx_price_history_product

#### Notification Log
- **Table**: `notification_log`
- **Purpose**: Prevent duplicate notifications (deduplication)
- **Fields**: id, user_id, notification_type (REMINDER/PRICE_DROP), reference_id, channel, recipient, sent_at, status (SUCCESS/FAILED), error_message
- **Indexes**: idx_notification_log_dedup

#### Notification Preferences
- **Table**: `notification_preferences`
- **Purpose**: Per-user notification settings
- **Fields**: id, user_id, reminders_enabled, price_drop_alerts_enabled, reminder_days_before (array), preferred_channel, created_at, updated_at
- **Related Table**: `reminder_days_before` (ElementCollection)

## API Endpoints

### Recipients Management

#### Get User Recipients
```
GET /api/recipients
Authorization: Bearer <token>
Response: List<RecipientResponse>
```

#### Get Single Recipient
```
GET /api/recipients/{id}
Authorization: Bearer <token>
Response: RecipientResponse
```

#### Create Recipient
```
POST /api/recipients
Authorization: Bearer <token>
Body: {
  "name": "John Doe",
  "description": "My best friend",
  "birthday": "1990-05-15"
}
Response: RecipientResponse (201 Created)
```

#### Update Recipient
```
PUT /api/recipients/{id}
Authorization: Bearer <token>
Body: RecipientRequest
Response: RecipientResponse
```

#### Delete Recipient
```
DELETE /api/recipients/{id}
Authorization: Bearer <token>
Response: 204 No Content
```

### Important Dates Management

#### Get User Dates
```
GET /api/important-dates
Authorization: Bearer <token>
Response: List<ImportantDateResponse>
```

#### Get Single Date
```
GET /api/important-dates/{id}
Authorization: Bearer <token>
Response: ImportantDateResponse
```

#### Create Important Date
```
POST /api/important-dates
Authorization: Bearer <token>
Body: {
  "name": "John's Birthday",
  "type": "BIRTHDAY",
  "date": "2025-05-15",
  "recurring": true,
  "description": "Buy something cool",
  "recipientId": 1
}
Response: ImportantDateResponse (201 Created)
```

#### Update Important Date
```
PUT /api/important-dates/{id}
Authorization: Bearer <token>
Body: ImportantDateRequest
Response: ImportantDateResponse
```

#### Delete Important Date
```
DELETE /api/important-dates/{id}
Authorization: Bearer <token>
Response: 204 No Content
```

### Saved Products Management

#### Get User Products
```
GET /api/saved-products
Authorization: Bearer <token>
Response: List<SavedProductResponse>
```

#### Get Single Product
```
GET /api/saved-products/{id}
Authorization: Bearer <token>
Response: SavedProductResponse
```

#### Save Product
```
POST /api/saved-products
Authorization: Bearer <token>
Body: {
  "productId": "MLA123456",
  "title": "Wireless Headphones",
  "currentPrice": 12500.00,
  "currency": "ARS",
  "productUrl": "https://...",
  "imageUrl": "https://...",
  "store": "MercadoLibre",
  "recipientId": 1,
  "priceTrackingEnabled": true,
  "priceDropThresholdPercent": 10.0
}
Response: SavedProductResponse (201 Created)
```

#### Update Product
```
PUT /api/saved-products/{id}
Authorization: Bearer <token>
Body: SavedProductRequest
Response: SavedProductResponse
```

#### Delete Product
```
DELETE /api/saved-products/{id}
Authorization: Bearer <token>
Response: 204 No Content
```

### Notification Preferences

#### Get User Preferences
```
GET /api/notification-preferences
Authorization: Bearer <token>
Response: NotificationPreferencesResponse
```

#### Update Preferences
```
PUT /api/notification-preferences
Authorization: Bearer <token>
Body: {
  "remindersEnabled": true,
  "priceDropAlertsEnabled": true,
  "reminderDaysBefore": [14, 7, 2],
  "preferredChannel": "EMAIL"
}
Response: NotificationPreferencesResponse
```

### Admin Endpoints (Require ADMIN role)

#### Get Job Status
```
GET /api/admin/job-status
Authorization: Bearer <admin-token>
Response: {
  "reminderGenerationJob": "Active",
  "reminderSendJob": "Active",
  "priceCheckJob": "Active",
  "schedulerStatus": "Running"
}
```

#### Get Reminder Queue
```
GET /api/admin/reminders/queue?status=PENDING
Authorization: Bearer <admin-token>
Response: List<ReminderQueueResponse>
```

#### Get Upcoming Reminders
```
GET /api/admin/reminders/upcoming?days=7
Authorization: Bearer <admin-token>
Response: List<ReminderQueueResponse>
```

#### Get Reminder Statistics
```
GET /api/admin/reminders/stats
Authorization: Bearer <admin-token>
Response: {
  "total": 150,
  "pending": 20,
  "sent": 120,
  "failed": 5,
  "cancelled": 5
}
```

## Scheduled Jobs

### Reminder Generation Job
- **Schedule**: Daily at 6 AM (configurable via `REMINDER_JOB_CRON`)
- **Purpose**: Generate reminder instances for upcoming important dates
- **Process**:
  1. Fetch all important dates in the next 30 days
  2. For each date, check user's notification preferences
  3. Generate reminders based on configured days before (e.g., 14, 7, 2 days)
  4. Ensure idempotency - don't create duplicate reminders
  5. Log generation statistics

### Reminder Send Job
- **Schedule**: Daily at 6 AM (configurable via `REMINDER_JOB_CRON`)
- **Purpose**: Send due reminders to users
- **Process**:
  1. Fetch all reminders scheduled for today
  2. Check for recent duplicates (within 24 hours)
  3. Send email notifications using templates
  4. Update reminder status (SENT/FAILED)
  5. Log notification to prevent duplicates
  6. Report success/failure counts

### Price Check Job
- **Schedule**: Every 12 hours (configurable via `PRICE_CHECK_JOB_CRON`)
- **Purpose**: Check for price drops on saved products
- **Process**:
  1. Fetch all products with price tracking enabled
  2. Check user's price drop alert preferences
  3. Calculate price drop percentage from latest history
  4. If drop exceeds threshold, send notification
  5. Check for recent duplicates
  6. Create new price history entry
  7. Report check statistics

## Email Templates

### Reminder Email
- **File**: `templates/email/reminder-email.html`
- **Variables**:
  - `userName`: User's name
  - `eventName`: Name of the event
  - `eventDate`: Date of the event
  - `daysUntil`: Days until the event
  - `recipientName`: (Optional) Name of recipient
  - `appUrl`: Application URL for "Find Gift Ideas" button

### Price Drop Email
- **File**: `templates/email/price-drop-email.html`
- **Variables**:
  - `userName`: User's name
  - `productTitle`: Product title
  - `productUrl`: Product URL
  - `productImageUrl`: (Optional) Product image
  - `oldPrice`: Previous price
  - `newPrice`: Current price
  - `currency`: Currency code
  - `dropPercentage`: Percentage discount
  - `savingsAmount`: Amount saved

## Configuration

### Environment Variables

Add these to your `.env` file:

```bash
# Email/SMTP Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
# For Gmail: Use App Password, not regular password
# For other providers: Use appropriate SMTP settings

# Application URLs
APP_BASE_URL=http://localhost:5173

# Scheduler Configuration (Cron expressions)
REMINDER_JOB_CRON=0 0 6 * * *          # Daily at 6 AM
PRICE_CHECK_JOB_CRON=0 0 */12 * * *    # Every 12 hours
```

### Spring Application Properties

The following are configured in `application.yml`:

```yaml
spring:
  mail:
    host: ${SMTP_HOST:smtp.gmail.com}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
  task:
    scheduling:
      pool:
        size: 2

app:
  base-url: ${APP_BASE_URL:http://localhost:5173}
  
scheduler:
  reminders:
    cron: ${REMINDER_JOB_CRON:0 0 6 * * *}
  price-check:
    cron: ${PRICE_CHECK_JOB_CRON:0 0 */12 * * *}
```

## Setup Instructions

### 1. Email Configuration

#### For Gmail:
1. Enable 2-Factor Authentication on your Google Account
2. Generate an App Password:
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" and your device
   - Copy the generated password
3. Add to `.env`:
   ```
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_USERNAME=your-email@gmail.com
   SMTP_PASSWORD=your-app-password
   ```

#### For Other Providers:
- **Outlook/Office365**:
  ```
  SMTP_HOST=smtp.office365.com
  SMTP_PORT=587
  ```
- **SendGrid**:
  ```
  SMTP_HOST=smtp.sendgrid.net
  SMTP_PORT=587
  SMTP_USERNAME=apikey
  SMTP_PASSWORD=your-api-key
  ```

### 2. Database Migration

Tables will be created automatically via JPA with `spring.jpa.hibernate.ddl-auto=update`.

For production, consider using Flyway or Liquibase for controlled migrations.

### 3. Testing Email Setup

Test email configuration using admin endpoints or by creating a test reminder.

## Security Considerations

1. **Email Credentials**: Never commit SMTP credentials to version control
2. **Admin Endpoints**: Protected with `@PreAuthorize("hasRole('ADMIN')")`
3. **User Data Isolation**: All queries filter by authenticated user
4. **Input Validation**: All request DTOs use `@Valid` annotation
5. **Deduplication**: Prevents sending duplicate notifications within 24 hours
6. **Rate Limiting**: Consider adding rate limits for API endpoints in production

## Observability

### Logging

All scheduled jobs log:
- Start and end times
- Number of items processed
- Success and failure counts
- Duration in milliseconds
- Detailed errors for failures

Example log output:
```
INFO  - Starting reminder generation job
INFO  - Found 25 upcoming dates to process
INFO  - Reminder generation job completed successfully. Generated 50 new reminders in 234ms

INFO  - Starting reminder send job
INFO  - Found 10 due reminders to send
INFO  - Reminder send job completed. Sent: 9, Failed: 1, Duration: 1523ms
```

### Monitoring

Use admin endpoints to monitor:
- Job status
- Reminder queue size
- Success/failure rates
- Upcoming reminder schedule

## Troubleshooting

### Emails Not Sending

1. **Check SMTP Configuration**:
   ```bash
   curl http://localhost:8080/api/admin/job-status
   ```

2. **Verify Email Credentials**: Test with a mail client

3. **Check Application Logs**:
   ```bash
   docker-compose logs backend | grep -i "email\|smtp\|notification"
   ```

4. **Common Issues**:
   - Gmail: Use App Password, not regular password
   - Firewall: Ensure port 587 is open
   - TLS/SSL: Verify `starttls.enable=true`

### Reminders Not Generated

1. **Check Important Dates**:
   ```bash
   curl -H "Authorization: Bearer <token>" http://localhost:8080/api/important-dates
   ```

2. **Check Notification Preferences**:
   ```bash
   curl -H "Authorization: Bearer <token>" http://localhost:8080/api/notification-preferences
   ```

3. **Verify Scheduler is Running**:
   ```bash
   docker-compose logs backend | grep -i "reminder generation job"
   ```

### Price Alerts Not Working

1. **Check Price Tracking Enabled**:
   ```bash
   curl -H "Authorization: Bearer <token>" http://localhost:8080/api/saved-products
   ```

2. **Verify Price History**:
   - Check database: `SELECT * FROM price_history WHERE saved_product_id = ?`

3. **Check Job Logs**:
   ```bash
   docker-compose logs backend | grep -i "price check job"
   ```

## Future Enhancements

1. **Multiple Notification Channels**:
   - Push notifications
   - WhatsApp integration
   - SMS alerts

2. **Advanced Price Tracking**:
   - Real-time price fetching from scraper
   - Price drop predictions
   - Historical price charts

3. **Smart Reminders**:
   - AI-suggested gift ideas based on recipient
   - Budget tracking
   - Gift purchase confirmation

4. **Analytics**:
   - User engagement metrics
   - Notification effectiveness
   - Gift preference insights

## API Testing

Use the following curl commands to test the APIs:

### Create a Recipient
```bash
curl -X POST http://localhost:8080/api/recipients \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "description": "Sister",
    "birthday": "1992-08-20"
  }'
```

### Create an Important Date
```bash
curl -X POST http://localhost:8080/api/important-dates \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Birthday",
    "type": "BIRTHDAY",
    "date": "2025-08-20",
    "recurring": true,
    "recipientId": 1
  }'
```

### Save a Product
```bash
curl -X POST http://localhost:8080/api/saved-products \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "MLA123456",
    "title": "Wireless Headphones",
    "currentPrice": 12500.00,
    "currency": "ARS",
    "productUrl": "https://articulo.mercadolibre.com.ar/...",
    "priceTrackingEnabled": true,
    "priceDropThresholdPercent": 10.0,
    "recipientId": 1
  }'
```

### Update Notification Preferences
```bash
curl -X PUT http://localhost:8080/api/notification-preferences \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "remindersEnabled": true,
    "priceDropAlertsEnabled": true,
    "reminderDaysBefore": [14, 7, 2],
    "preferredChannel": "EMAIL"
  }'
```

## Conclusion

The proactive automation module adds powerful reminder and price tracking capabilities to GiftFinder AI, helping users never miss an important occasion and save money on gifts.
