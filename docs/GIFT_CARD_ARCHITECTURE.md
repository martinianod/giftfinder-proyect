# Gift Card System Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        GiftFinder Frontend                       │
│                         (React App)                              │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 │ HTTPS/REST
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Layer (Spring Boot)                      │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ GiftCardController│  │ WalletController │  │AdminController│ │
│  │                   │  │                  │  │              │  │
│  │ POST /giftcards   │  │ GET /wallet      │  │POST .../cancel│ │
│  │ POST .../redeem   │  │                  │  │POST .../expire│ │
│  │ GET /giftcards    │  │                  │  │              │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 │ Service Calls
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────────┐ │
│  │           GiftCardService                                   │ │
│  │  • createGiftCard()  • redeemGiftCard()                    │ │
│  │  • sendGiftCard()    • cancelGiftCard()                    │ │
│  │  • expireGiftCard()  • processExpiredGiftCards()           │ │
│  └────────────────────────────────────────────────────────────┘ │
│                           │         │                            │
│                           │         └──────────────┐             │
│                           ▼                        ▼             │
│  ┌────────────────────────────────┐  ┌─────────────────────┐   │
│  │      WalletService             │  │ NotificationService │   │
│  │  • getOrCreateWallet()         │  │  • sendEmail()      │   │
│  │  • credit()                    │  │                     │   │
│  │  • debit()                     │  │                     │   │
│  │  • getBalance()                │  │                     │   │
│  │  • getLedgerEntries()          │  │                     │   │
│  │  • transactionExists()         │  │                     │   │
│  └────────────────────────────────┘  └─────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 │ JPA/Hibernate
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Data Access Layer                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │GiftCardRepository│  │ WalletRepository │  │WalletLedger- │  │
│  │                  │  │                  │  │EntryRepository│ │
│  │ JpaRepository    │  │ JpaRepository    │  │JpaRepository │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                 │
                                 │ JDBC
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Database (PostgreSQL)                         │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │   gift_cards     │  │     wallets      │  │wallet_ledger_│  │
│  │                  │  │                  │  │  entries     │  │
│  │ • id (PK)        │  │ • id (PK)        │  │ • id (PK)    │  │
│  │ • code (UK)      │  │ • user_id (UK,FK)│  │ • wallet_id  │  │
│  │ • hashed_code    │  │ • balance        │  │ • amount     │  │
│  │ • amount         │  │ • currency       │  │ • type       │  │
│  │ • currency       │  │ • version        │  │ • source_type│  │
│  │ • sender_id (FK) │  │                  │  │ • reference_id│ │
│  │ • recipient_email│  │                  │  │              │  │
│  │ • status         │  │                  │  │              │  │
│  │ • expiry_date    │  │                  │  │              │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Background Jobs                               │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  GiftCardExpirationJob                                      │ │
│  │  • Cron: Daily at 3 AM                                      │ │
│  │  • Processes expired gift cards                             │ │
│  │  • Updates status: CREATED/SENT → EXPIRED                   │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Component Descriptions

### API Layer
**Controllers** handle HTTP requests and responses, validate input, and delegate to services.

- **GiftCardController**: Manages gift card lifecycle operations
  - Creates new gift cards
  - Redeems gift cards
  - Lists user's sent gift cards
  
- **WalletController**: Manages user wallet operations
  - Retrieves wallet balance and transactions
  
- **AdminController**: Administrative operations
  - Cancels gift cards
  - Expires gift cards

### Service Layer
**Services** contain business logic, transaction management, and coordinate between repositories.

- **GiftCardService**: Core gift card operations
  - Generates secure unique codes (UUID)
  - Manages gift card status lifecycle
  - Coordinates redemption with wallet service
  - Handles email sending
  - Processes expiration
  
- **WalletService**: Wallet and ledger management
  - Manages wallet creation and balance
  - Atomic credit/debit operations
  - Idempotency checks via ledger
  - Transaction history
  
- **NotificationService**: Email notifications
  - Sends gift card emails to recipients
  - Uses Thymeleaf templates

### Data Access Layer
**Repositories** provide CRUD operations and custom queries using Spring Data JPA.

- **GiftCardRepository**: Gift card persistence
  - Find by code, sender, recipient email
  - Custom queries for filtering
  
- **WalletRepository**: Wallet persistence
  - Find by user
  - One wallet per user
  
- **WalletLedgerEntryRepository**: Ledger persistence
  - Find by wallet, reference ID
  - Immutable audit trail

### Database Layer
**PostgreSQL** stores persistent data with proper constraints and indexes.

- **gift_cards**: Gift card records with status tracking
- **wallets**: User wallet balances with optimistic locking
- **wallet_ledger_entries**: Immutable transaction log

### Background Jobs
**Scheduled Jobs** run periodic maintenance tasks.

- **GiftCardExpirationJob**: Daily expiration processing

## Data Flow Diagrams

### 1. Create Gift Card Flow

```
User → Frontend → POST /api/giftcards
                        ↓
                  GiftCardController
                        ↓
                  (validate input)
                        ↓
                  GiftCardService
                        ↓
                  (generate UUID code)
                        ↓
                  (hash code with BCrypt)
                        ↓
                  GiftCardRepository.save()
                        ↓
                  PostgreSQL (INSERT gift_cards)
                        ↓
              (if immediate delivery)
                        ↓
                  NotificationService
                        ↓
                  (send email to recipient)
                        ↓
                  Response → Frontend → User
```

### 2. Redeem Gift Card Flow

```
User → Frontend → POST /api/giftcards/redeem {code}
                        ↓
                  GiftCardController
                        ↓
                  GiftCardService.redeemGiftCard()
                        ↓
            ┌───────────┴───────────┐
            ▼                       ▼
   GiftCardRepository      WalletService
   .findByCode()           .transactionExists()
            │                       │
            │                  (check idempotency)
            ▼                       │
   (validate status)                │
   (check expiry)                   │
            │                       ▼
            │              WalletService.credit()
            │                       │
            │              ┌────────┴────────┐
            │              ▼                 ▼
            │      WalletRepository  WalletLedgerEntry-
            │      .save()           Repository.save()
            │              │                 │
            │              ▼                 ▼
            │       UPDATE wallets    INSERT ledger
            │                               │
            ▼                               │
   GiftCardRepository.save()                │
   (status = REDEEMED)                      │
            │                               │
            └───────────┬───────────────────┘
                        ▼
                 Response → Frontend → User
```

### 3. Wallet Query Flow

```
User → Frontend → GET /api/wallet
                        ↓
                  WalletController
                        ↓
                  WalletService.getOrCreateWallet()
                        ↓
                  WalletRepository.findByUser()
                        ↓
                  (if not exists, create new)
                        ↓
                  WalletService.getLedgerEntries()
                        ↓
            WalletLedgerEntryRepository.findByWallet()
                        ↓
                  PostgreSQL (SELECT)
                        ↓
                  (map to DTOs)
                        ↓
                  Response → Frontend → User
                  {
                    balance: 100,
                    currency: "ARS",
                    recentTransactions: [...]
                  }
```

### 4. Scheduled Expiration Flow

```
Cron Trigger (3 AM daily)
            ↓
    GiftCardExpirationJob
            ↓
    GiftCardService.processExpiredGiftCards()
            ↓
    GiftCardRepository.findAll()
            ↓
    (filter expired: expiry_date < today)
            ↓
    (filter non-final status)
            ↓
    For each expired card:
            ↓
    (update status = EXPIRED)
            ↓
    GiftCardRepository.save()
            ↓
    PostgreSQL (UPDATE gift_cards)
            ↓
    Log completion
```

## Security Architecture

### 1. Code Generation & Storage

```
User Request
     ↓
Generate UUID
     ↓
Format: 16 chars uppercase alphanumeric
     ↓
Check uniqueness (loop if duplicate)
     ↓
┌──────────┴──────────┐
▼                     ▼
Plain Text          BCrypt Hash
(for display)       (for validation)
     │                     │
     └──────────┬──────────┘
                ↓
        Store both in DB
                ↓
        gift_cards table
        • code (plain)
        • hashed_code (bcrypt)
```

**Rationale**: 
- Plain text needed for display and emails
- Hashed version provides defense-in-depth
- If DB compromised, hashed codes still protected

### 2. Redemption Idempotency

```
Redeem Request
     ↓
Check WalletLedgerEntry
     ↓
referenceId = gift_card_code
sourceType = GIFT_CARD_REDEMPTION
     ↓
EXISTS?
     │
     ├─→ YES → Return success (no action)
     │
     └─→ NO  → Continue redemption
                     ↓
             @Transactional
                     ↓
             ┌───────┴───────┐
             ▼               ▼
      Credit Wallet    Create Ledger Entry
             │               │
             └───────┬───────┘
                     ↓
             Update Gift Card
             (status = REDEEMED)
                     ↓
             Commit Transaction
```

### 3. Optimistic Locking

```
Thread A                    Thread B
   │                           │
   ├─→ Read Wallet (v=1)       │
   │   balance = 100           │
   │                           ├─→ Read Wallet (v=1)
   │                           │   balance = 100
   ├─→ Credit +50              │
   │   balance = 150           ├─→ Credit +30
   │   version = 2             │   balance = 130
   │                           │   version = 2
   ├─→ Save (v=1 → v=2) ✅     │
   │   SUCCESS                 │
   │                           ├─→ Save (v=1 → v=2) ❌
   │                           │   CONFLICT!
   │                           │   (version mismatch)
   │                           │
   │                           └─→ Retry with v=2
                                   Read: 150
                                   Credit +30 = 180
                                   Save (v=2 → v=3) ✅
```

## Scalability Considerations

### Database Indexes

```
gift_cards
├── PRIMARY KEY (id)
├── UNIQUE INDEX (code)
├── INDEX (sender_user_id)
├── INDEX (recipient_email)
├── INDEX (status)
└── INDEX (expiry_date)

wallets
├── PRIMARY KEY (id)
└── UNIQUE INDEX (user_id)

wallet_ledger_entries
├── PRIMARY KEY (id)
├── INDEX (wallet_id)
├── INDEX (reference_id)
├── INDEX (created_at DESC)
├── INDEX (source_type)
└── UNIQUE INDEX (reference_id, source_type)  # Idempotency
```

### Expected Growth

```
Year 1:
  Users: 10,000
  Gift Cards: 5,000
  Wallets: 8,000
  Ledger Entries: 15,000

Year 5:
  Users: 100,000
  Gift Cards: 50,000
  Wallets: 80,000
  Ledger Entries: 200,000
```

### Performance Optimizations

1. **Caching** (Future)
   - Cache wallet balances (Redis)
   - Cache active gift card counts
   - Invalidate on updates

2. **Partitioning** (High Volume)
   - Partition ledger by date
   - Archive old gift cards

3. **Connection Pooling**
   - HikariCP (Spring Boot default)
   - Min: 10, Max: 20 connections

4. **Query Optimization**
   - Fetch only necessary columns
   - Use DTOs to avoid N+1 queries
   - Paginate large results

## Error Handling

### Error Flow

```
Request
   ↓
Controller
   ↓
(validation errors?)
   │
   ├─→ YES → 400 Bad Request
   │         { field, message }
   │
   └─→ NO  → Service
               ↓
      (business rule violation?)
               │
               ├─→ YES → throw Exception
               │         ↓
               │    @ExceptionHandler
               │         ↓
               │    400/409/500
               │
               └─→ NO  → Repository
                           ↓
                  (database error?)
                           │
                           ├─→ YES → 500 Internal Error
                           │
                           └─→ NO  → Success
                                      ↓
                                   Response
```

### Error Codes

| Status | Error | Description |
|--------|-------|-------------|
| 400 | Bad Request | Invalid input, validation failed |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Version conflict, already redeemed |
| 500 | Internal Error | Server error |

## Monitoring & Observability

### Logging Levels

```
TRACE: Not used
DEBUG: Detailed operation logs
INFO:  Key operations (create, redeem, expire)
WARN:  Unusual but handled (already sent, already redeemed)
ERROR: Failures requiring attention
```

### Key Metrics to Monitor

1. **Gift Card Metrics**
   - Cards created per day
   - Redemption rate
   - Average time to redemption
   - Expiration rate

2. **Wallet Metrics**
   - Active wallets
   - Average balance
   - Total balance across all wallets
   - Transaction volume

3. **Performance Metrics**
   - API response times
   - Database query times
   - Error rates
   - Scheduled job duration

4. **Business Metrics**
   - Total gift card value issued
   - Total redeemed value
   - Unredeemed balance
   - Revenue (if applicable)

## Deployment Architecture

### Production Setup

```
               ┌─────────────┐
               │   Nginx     │
               │  (SSL/TLS)  │
               └─────────────┘
                      │
              Load Balancer
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
    ┌───────────┐           ┌───────────┐
    │ Backend 1 │           │ Backend 2 │
    │  (Java)   │           │  (Java)   │
    └───────────┘           └───────────┘
          │                       │
          └───────────┬───────────┘
                      │
                ┌─────────────┐
                │ PostgreSQL  │
                │  (Primary)  │
                └─────────────┘
                      │
                ┌─────────────┐
                │ PostgreSQL  │
                │  (Replica)  │
                └─────────────┘
```

## Conclusion

This architecture provides:

- ✅ **Scalability**: Supports growth to 100K+ users
- ✅ **Security**: Multiple layers of protection
- ✅ **Reliability**: Transaction guarantees, idempotency
- ✅ **Maintainability**: Clear separation of concerns
- ✅ **Observability**: Comprehensive logging and metrics
- ✅ **Performance**: Optimized queries and indexes
