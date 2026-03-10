-- =====================================================================
-- Create 100 Test Wallets for k6 Performance Testing
-- =====================================================================
-- Purpose: Prepare test data for wallet concurrent debit performance test
--
-- Creates:
--   - 100 test players (test_player_001 ~ test_player_100)
--   - 100 CASH wallets (10,000 CNY each)
--   - Total initial balance: 1,000,000 CNY
--
-- Prerequisites:
--   - SmartAdmin application must be running
--   - PostgreSQL database 'smartadmin_igaming' must exist
--   - Flyway migrations V1-V16 must be applied
--
-- Usage:
--   psql -h localhost -U smartadmin_user -d smartadmin_igaming -f create-test-wallets.sql
-- =====================================================================

-- Enable pgcrypto extension for digest() function (SHA-256 blind index)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- Cleanup: Delete existing test data before creating new test wallets
-- =====================================================================
DELETE FROM t_wallet
WHERE player_id IN (
    SELECT player_id FROM t_player
    WHERE username LIKE 'test_player_%' AND tenant_id = 1
);

DELETE FROM t_player
WHERE username LIKE 'test_player_%' AND tenant_id = 1;

DO $$
DECLARE
    i INT;
    v_player_id BIGINT;
    v_wallet_id BIGINT;
    v_created_count INT := 0;
BEGIN
    -- Set tenant context for Multi-Tenant Row-Level Security
    PERFORM set_config('app.current_tenant_id', '1', false);

    RAISE NOTICE '==========================================';
    RAISE NOTICE 'Starting Test Wallet Creation';
    RAISE NOTICE 'Target: 100 wallets × 10,000 CNY = 1,000,000 CNY';
    RAISE NOTICE '==========================================';

    -- Loop to create 100 test players and wallets
    FOR i IN 1..100 LOOP
        -- Insert test player
        INSERT INTO t_player (
            username,
            password_hash,
            email_encrypted,
            email_blind_idx,
            phone_encrypted,
            phone_blind_idx,
            status,
            kyc_level,
            vip_level,
            registration_ip,
            last_login_ip,
            deleted,
            version,
            tenant_id,
            create_time,
            update_time
        )
        VALUES (
            'test_player_' || LPAD(i::TEXT, 3, '0'),
            -- Argon2id dummy hash (not for production use)
            '$argon2id$v=19$m=65536,t=3,p=4$c29tZXNhbHQ$dummyhashfortest',
            -- Email (will be encrypted by EncryptedFieldTypeHandler)
            'test' || i || '@k6test.com',
            -- Email blind index (HMAC-SHA256 for equality lookup)
            encode(digest('test' || i || '@k6test.com', 'sha256'), 'hex'),
            -- Phone (will be encrypted)
            '+86138' || LPAD(i::TEXT, 8, '0'),
            -- Phone blind index
            encode(digest('+86138' || LPAD(i::TEXT, 8, '0'), 'sha256'), 'hex'),
            1,  -- status: ACTIVE
            1,  -- kyc_level: L1 (Basic Verification)
            1,  -- vip_level: VIP 1
            '127.0.0.1',  -- registration_ip
            '127.0.0.1',  -- last_login_ip
            FALSE,  -- deleted
            0,      -- version (optimistic lock)
            1,      -- tenant_id
            NOW(),
            NOW()
        )
        RETURNING player_id INTO v_player_id;

        -- Insert CASH wallet for this player
        INSERT INTO t_wallet (
            player_id,
            wallet_type,
            balance,
            locked_amount,
            currency_code,
            status,
            deleted,
            version,
            tenant_id,
            create_time,
            update_time
        )
        VALUES (
            v_player_id,
            1,  -- wallet_type: CASH
            10000.0000,  -- balance: 10,000 CNY
            0.0000,      -- locked_amount: 0 (no funds locked initially)
            'CNY',       -- currency_code
            1,           -- status: ACTIVE
            FALSE,       -- deleted
            0,           -- version (optimistic lock)
            1,           -- tenant_id
            NOW(),
            NOW()
        )
        RETURNING wallet_id INTO v_wallet_id;

        v_created_count := v_created_count + 1;

        -- Log progress every 10 wallets
        IF v_created_count % 10 = 0 THEN
            RAISE NOTICE 'Created % test wallets... (Player ID: %, Wallet ID: %)',
                v_created_count, v_player_id, v_wallet_id;
        END IF;
    END LOOP;

    -- Final summary
    RAISE NOTICE '==========================================';
    RAISE NOTICE '✅ Test Wallet Creation Complete';
    RAISE NOTICE 'Total created: % wallets', v_created_count;
    RAISE NOTICE 'Total balance: % CNY', v_created_count * 10000;
    RAISE NOTICE '==========================================';

    -- Verify creation
    PERFORM 1
    FROM t_wallet
    WHERE currency_code = 'CNY' AND deleted = FALSE
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Verification failed: No wallets found after creation';
    END IF;
END $$;

-- =====================================================================
-- Verification Query
-- =====================================================================

SELECT
    COUNT(*) AS wallet_count,
    SUM(balance) AS total_balance,
    MIN(balance) AS min_balance,
    MAX(balance) AS max_balance,
    AVG(balance) AS avg_balance
FROM t_wallet
WHERE currency_code = 'CNY' AND deleted = FALSE;

-- Expected output:
--  wallet_count | total_balance | min_balance | max_balance |   avg_balance
-- --------------+---------------+-------------+-------------+------------------
--           100 |  1000000.0000 | 10000.0000  | 10000.0000  | 10000.0000000000
