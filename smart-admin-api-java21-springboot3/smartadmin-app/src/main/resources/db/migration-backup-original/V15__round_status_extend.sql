-- =====================================================================
-- V15: Extend Game Round Status — Sprint 2 Seamless Wallet Deep Impl
-- =====================================================================
-- Adds 3 new round statuses: TIMEOUT(5), ADJUSTED(6), PENDING_REVIEW(7)
-- Required for 6-step atomic operation lifecycle.
-- =====================================================================

-- Drop existing CHECK constraint and recreate with new values
ALTER TABLE t_game_round DROP CONSTRAINT ck_round_status;
ALTER TABLE t_game_round ADD CONSTRAINT ck_round_status
    CHECK (status IN (1, 2, 3, 4, 5, 6, 7));

COMMENT ON COLUMN t_game_round.status IS '局狀態: 1=進行中, 2=已結算, 3=已取消, 4=已作廢, 5=逾時, 6=已調整, 7=待審核';
