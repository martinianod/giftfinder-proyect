# Gift Card System Database Migration Guide

## Overview

This guide provides SQL scripts to set up the database schema for the Gift Card System. The application uses Hibernate's `ddl-auto=update` mode, which will automatically create tables on startup, but this guide provides explicit SQL for manual migration or production deployment.

## Prerequisites

- PostgreSQL 12+
- Database user with CREATE TABLE privileges
- Existing `users` table (from GiftFinder authentication system)

## Migration Steps

### Step 1: Backup Current Database

Before applying any migrations, create a backup:

```bash
pg_dump -U giftfinder_user giftfinder > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Step 2: Create Gift Cards Table

```sql
-- Create gift_cards table
CREATE TABLE gift_cards (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    hashed_code VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    message VARCHAR(500),
    sender_user_id BIGINT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    delivery_date DATE,
    status VARCHAR(50) NOT NULL,
    expiry_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    redeemed_at TIMESTAMP,
    redeemed_by_user_id BIGINT,
    
    -- Foreign key constraints
    CONSTRAINT fk_gift_cards_sender 
        FOREIGN KEY (sender_user_id) 
        REFERENCES users(id) 
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_gift_cards_redeemed_by 
        FOREIGN KEY (redeemed_by_user_id) 
        REFERENCES users(id) 
        ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT check_status 
        CHECK (status IN ('CREATED', 'SENT', 'REDEEMED', 'EXPIRED', 'CANCELLED')),
    
    CONSTRAINT check_currency_length 
        CHECK (LENGTH(currency) = 3),
    
    CONSTRAINT check_dates 
        CHECK (created_at <= updated_at)
);

-- Create indexes for performance
CREATE INDEX idx_gift_cards_code ON gift_cards(code);
CREATE INDEX idx_gift_cards_sender_user_id ON gift_cards(sender_user_id);
CREATE INDEX idx_gift_cards_recipient_email ON gift_cards(recipient_email);
CREATE INDEX idx_gift_cards_status ON gift_cards(status);
CREATE INDEX idx_gift_cards_expiry_date ON gift_cards(expiry_date);

-- Comment the table and columns
COMMENT ON TABLE gift_cards IS 'Stores gift card information including codes, amounts, and status';
COMMENT ON COLUMN gift_cards.code IS 'Plain text gift card code for user display (16 chars, alphanumeric)';
COMMENT ON COLUMN gift_cards.hashed_code IS 'BCrypt hashed version of the code for security';
COMMENT ON COLUMN gift_cards.status IS 'Current status: CREATED, SENT, REDEEMED, EXPIRED, or CANCELLED';
```

### Step 3: Create Wallets Table

```sql
-- Create wallets table
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT fk_wallets_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT check_currency_length 
        CHECK (LENGTH(currency) = 3)
);

-- Create indexes
CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Comment the table and columns
COMMENT ON TABLE wallets IS 'User wallet balances from redeemed gift cards';
COMMENT ON COLUMN wallets.balance IS 'Current wallet balance (always >= 0)';
COMMENT ON COLUMN wallets.version IS 'Optimistic locking version for concurrent updates';
```

### Step 4: Create Wallet Ledger Entries Table

```sql
-- Create wallet_ledger_entries table
CREATE TABLE wallet_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_wallet_ledger_entries_wallet 
        FOREIGN KEY (wallet_id) 
        REFERENCES wallets(id) 
        ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT check_transaction_type 
        CHECK (type IN ('CREDIT', 'DEBIT')),
    
    CONSTRAINT check_source_type 
        CHECK (source_type IN ('GIFT_CARD_REDEMPTION', 'PURCHASE', 'REFUND', 'ADJUSTMENT'))
);

-- Create indexes for performance
CREATE INDEX idx_wallet_ledger_entries_wallet_id ON wallet_ledger_entries(wallet_id);
CREATE INDEX idx_wallet_ledger_entries_reference_id ON wallet_ledger_entries(reference_id);
CREATE INDEX idx_wallet_ledger_entries_created_at ON wallet_ledger_entries(created_at DESC);
CREATE INDEX idx_wallet_ledger_entries_source_type ON wallet_ledger_entries(source_type);

-- Create unique index for idempotency
CREATE UNIQUE INDEX idx_wallet_ledger_idempotency 
    ON wallet_ledger_entries(reference_id, source_type);

-- Comment the table and columns
COMMENT ON TABLE wallet_ledger_entries IS 'Immutable ledger of all wallet transactions for audit trail';
COMMENT ON COLUMN wallet_ledger_entries.type IS 'CREDIT (add money) or DEBIT (remove money)';
COMMENT ON COLUMN wallet_ledger_entries.source_type IS 'Source of the transaction';
COMMENT ON COLUMN wallet_ledger_entries.reference_id IS 'External reference (e.g., gift card code, order ID)';
```

### Step 5: Verify Migration

```sql
-- Verify tables were created
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name IN ('gift_cards', 'wallets', 'wallet_ledger_entries');

-- Verify indexes were created
SELECT tablename, indexname 
FROM pg_indexes 
WHERE schemaname = 'public' 
  AND tablename IN ('gift_cards', 'wallets', 'wallet_ledger_entries')
ORDER BY tablename, indexname;

-- Verify foreign key constraints
SELECT
    tc.table_name,
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.table_schema = 'public'
  AND tc.table_name IN ('gift_cards', 'wallets', 'wallet_ledger_entries')
  AND tc.constraint_type = 'FOREIGN KEY'
ORDER BY tc.table_name;
```

## Rollback Procedure

If you need to rollback the migration:

```sql
-- Drop tables in reverse order (respecting foreign keys)
DROP TABLE IF EXISTS wallet_ledger_entries CASCADE;
DROP TABLE IF EXISTS wallets CASCADE;
DROP TABLE IF EXISTS gift_cards CASCADE;

-- Restore from backup
-- psql -U giftfinder_user giftfinder < backup_YYYYMMDD_HHMMSS.sql
```

## Data Migration (if applicable)

If you have existing data that needs to be migrated, here are some example queries:

### Migrate existing promotional credits to wallets

```sql
-- Example: If you have an old promotional_credits table
INSERT INTO wallets (user_id, balance, currency, created_at, updated_at, version)
SELECT 
    user_id,
    COALESCE(credit_amount, 0.00),
    'ARS',
    NOW(),
    NOW(),
    0
FROM promotional_credits
WHERE credit_amount > 0
ON CONFLICT (user_id) DO NOTHING;

-- Create ledger entries for migrated credits
INSERT INTO wallet_ledger_entries (wallet_id, amount, type, source_type, reference_id, description, created_at)
SELECT 
    w.id,
    pc.credit_amount,
    'CREDIT',
    'ADJUSTMENT',
    'MIGRATION_' || pc.id,
    'Migrated from promotional_credits',
    NOW()
FROM promotional_credits pc
JOIN wallets w ON w.user_id = pc.user_id
WHERE pc.credit_amount > 0;
```

## Performance Considerations

### Expected Table Sizes

Based on typical usage patterns:

- **gift_cards**: 
  - Growth rate: ~100-1000 rows per month
  - Estimated size after 1 year: 1,200-12,000 rows
  - Storage per 10,000 rows: ~2-3 MB

- **wallets**: 
  - Growth rate: One per user
  - Estimated size: Equal to user count
  - Storage per 10,000 rows: ~500 KB

- **wallet_ledger_entries**: 
  - Growth rate: 2-10 entries per gift card redemption + purchases
  - Estimated size after 1 year: 5,000-50,000 rows
  - Storage per 10,000 rows: ~1-2 MB

### Index Maintenance

For optimal performance, rebuild indexes periodically:

```sql
-- Rebuild indexes (recommended monthly for high-traffic systems)
REINDEX TABLE gift_cards;
REINDEX TABLE wallets;
REINDEX TABLE wallet_ledger_entries;

-- Analyze tables for query optimization
ANALYZE gift_cards;
ANALYZE wallets;
ANALYZE wallet_ledger_entries;
```

### Partitioning (Optional for Large Datasets)

If you expect high volume (millions of rows), consider partitioning:

```sql
-- Example: Partition wallet_ledger_entries by year
CREATE TABLE wallet_ledger_entries_2024 PARTITION OF wallet_ledger_entries
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE wallet_ledger_entries_2025 PARTITION OF wallet_ledger_entries
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
```

## Monitoring Queries

### Check gift card status distribution

```sql
SELECT status, COUNT(*) as count
FROM gift_cards
GROUP BY status
ORDER BY count DESC;
```

### Check total gift card value by status

```sql
SELECT status, 
       COUNT(*) as count,
       SUM(amount) as total_amount,
       currency
FROM gift_cards
GROUP BY status, currency
ORDER BY status;
```

### Check wallet balances

```sql
SELECT 
    COUNT(*) as total_wallets,
    SUM(balance) as total_balance,
    AVG(balance) as avg_balance,
    MAX(balance) as max_balance,
    currency
FROM wallets
GROUP BY currency;
```

### Check ledger entry distribution

```sql
SELECT 
    type,
    source_type,
    COUNT(*) as count,
    SUM(amount) as total_amount
FROM wallet_ledger_entries
GROUP BY type, source_type
ORDER BY count DESC;
```

### Find expired gift cards

```sql
SELECT COUNT(*) as expired_count,
       SUM(amount) as expired_amount
FROM gift_cards
WHERE status != 'REDEEMED' 
  AND status != 'CANCELLED'
  AND expiry_date < CURRENT_DATE;
```

## Security Considerations

### 1. Restrict Database Access

```sql
-- Create a read-only user for reporting
CREATE USER giftcard_reader WITH PASSWORD 'strong_password';
GRANT CONNECT ON DATABASE giftfinder TO giftcard_reader;
GRANT USAGE ON SCHEMA public TO giftcard_reader;
GRANT SELECT ON gift_cards, wallets, wallet_ledger_entries TO giftcard_reader;
```

### 2. Audit Trail

Enable PostgreSQL audit logging for these tables:

```sql
-- Example using pgaudit extension
CREATE EXTENSION IF NOT EXISTS pgaudit;

-- Audit all DML operations on gift card tables
ALTER TABLE gift_cards SET (log_statement = 'all');
ALTER TABLE wallets SET (log_statement = 'all');
ALTER TABLE wallet_ledger_entries SET (log_statement = 'all');
```

### 3. Encryption at Rest

Ensure your PostgreSQL instance has encryption at rest enabled. Consult your hosting provider's documentation.

## Troubleshooting

### Issue: Foreign key constraint violation

If you get foreign key errors during migration:

```sql
-- Check for orphaned records
SELECT gc.id, gc.sender_user_id
FROM gift_cards gc
LEFT JOIN users u ON gc.sender_user_id = u.id
WHERE u.id IS NULL;

-- Fix orphaned records (delete or assign to a system user)
DELETE FROM gift_cards WHERE sender_user_id NOT IN (SELECT id FROM users);
```

### Issue: Duplicate wallet entries

```sql
-- Find duplicate wallets
SELECT user_id, COUNT(*)
FROM wallets
GROUP BY user_id
HAVING COUNT(*) > 1;

-- Merge duplicates (keep the one with highest balance)
WITH duplicates AS (
    SELECT user_id, MAX(id) as keep_id
    FROM wallets
    GROUP BY user_id
    HAVING COUNT(*) > 1
)
DELETE FROM wallets w
WHERE EXISTS (
    SELECT 1 FROM duplicates d
    WHERE d.user_id = w.user_id 
      AND d.keep_id != w.id
);
```

## Production Deployment Checklist

- [ ] Backup current database
- [ ] Run migration scripts in a transaction
- [ ] Verify all tables and indexes created
- [ ] Test gift card creation and redemption
- [ ] Monitor database performance
- [ ] Set up automated backups
- [ ] Configure monitoring alerts
- [ ] Document rollback procedure
- [ ] Train support team on new features
- [ ] Update application configuration

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Gift Card API Documentation](./GIFT_CARD_API.md)
- [Application Configuration Guide](../README.md)
