-- =====================================================================
-- V18: Initialize iGaming menu tree and permission entries
-- =====================================================================
-- Creates the iGaming menu hierarchy (directories + pages + permission buttons)
-- corresponding to 39 unique @SaCheckPermission values across 14 controllers.
--
-- Menu types: 1=directory, 2=page, 3=permission button
-- perms_type: 1=backend permission
-- =====================================================================

-- ==================== Root Directory ====================
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
VALUES ('iGaming 管理', 1, 0, 100, '/igaming', NULL, false, false, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW());

-- ==================== Wallet Module ====================
-- Wallet Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '錢包管理', 2, m.menu_id, 1, '/igaming/wallet', 'igaming/wallet/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

-- Wallet permission buttons
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('餘額查詢', 1, 'wallet:balance:query'),
    ('餘額操作', 2, 'wallet:balance:operate'),
    ('加款操作', 3, 'wallet:credit:operate'),
    ('扣款操作', 4, 'wallet:debit:operate'),
    ('鎖定操作', 5, 'wallet:lock:operate'),
    ('交易查詢', 6, 'wallet:transaction:query')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '錢包管理' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- Payment Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '支付管理', 2, m.menu_id, 2, '/igaming/payment', 'igaming/payment/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('存款操作', 1, 'payment:deposit:operate'),
    ('提款操作', 2, 'payment:withdraw:operate'),
    ('訂單查詢', 3, 'payment:order:query')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '支付管理' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- Financial Report Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '財務報表', 2, m.menu_id, 3, '/igaming/report', 'igaming/report/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '報表查詢', 3, p.menu_id, 1, NULL, NULL, false, false, true, false,
    'wallet:report:query', 1, 'wallet:report:query', false, 1, 1, 1, NOW(), NOW()
FROM t_menu p WHERE p.menu_name = '財務報表' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- ==================== Game Module ====================
-- Game Lobby Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '遊戲大廳', 2, m.menu_id, 4, '/igaming/game/lobby', 'igaming/game/lobby/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('遊戲查詢', 1, 'game:lobby:query'),
    ('遊戲操作', 2, 'game:lobby:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '遊戲大廳' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- Game Provider Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '遊戲供應商', 2, m.menu_id, 5, '/igaming/game/provider', 'igaming/game/provider/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('供應商查詢', 1, 'game:provider:query'),
    ('供應商操作', 2, 'game:provider:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '遊戲供應商' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- ==================== Player Module ====================
-- Player Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '玩家管理', 2, m.menu_id, 6, '/igaming/player', 'igaming/player/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('玩家查詢', 1, 'player:info:query'),
    ('玩家操作', 2, 'player:info:operate'),
    ('KYC 查詢', 3, 'player:kyc:query'),
    ('KYC 審核', 4, 'player:kyc:review'),
    ('KYC 操作', 5, 'player:kyc:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '玩家管理' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- ==================== Activity Module ====================
-- Promotion Rules Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '促銷規則', 2, m.menu_id, 7, '/igaming/activity/promotion', 'igaming/activity/promotion/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('規則查詢', 1, 'activity:promotion:query'),
    ('規則新增', 2, 'activity:promotion:add'),
    ('規則修改', 3, 'activity:promotion:update')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '促銷規則' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- Bonus Claim Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '紅利管理', 2, m.menu_id, 8, '/igaming/activity/bonus', 'igaming/activity/bonus/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('紅利查詢', 1, 'activity:bonus:query'),
    ('紅利操作', 2, 'activity:bonus:operate')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '紅利管理' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- ==================== Risk Module ====================
-- Risk Rules Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '風控規則', 2, m.menu_id, 9, '/igaming/risk/rule', 'igaming/risk/rule/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('規則查詢', 1, 'risk:rule:query'),
    ('規則新增', 2, 'risk:rule:add'),
    ('規則修改', 3, 'risk:rule:update'),
    ('規則刪除', 4, 'risk:rule:delete')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '風控規則' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- Risk Proposals Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '風控提案', 2, m.menu_id, 10, '/igaming/risk/proposal', 'igaming/risk/proposal/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('提案查詢', 1, 'risk:proposal:query'),
    ('提案審核', 2, 'risk:proposal:review')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '風控提案' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- Risk Score Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '風險評分', 2, m.menu_id, 11, '/igaming/risk/score', 'igaming/risk/score/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '評分查詢', 3, p.menu_id, 1, NULL, NULL, false, false, true, false,
    'risk:score:query', 1, 'risk:score:query', false, 1, 1, 1, NOW(), NOW()
FROM t_menu p WHERE p.menu_name = '風險評分' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- ==================== Agent Module ====================
-- Affiliate Management Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '聯盟代理', 2, m.menu_id, 12, '/igaming/agent/affiliate', 'igaming/agent/affiliate/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('代理查詢', 1, 'agent:affiliate:query'),
    ('代理註冊', 2, 'agent:affiliate:register'),
    ('佣金管理', 3, 'agent:affiliate:commission')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '聯盟代理' AND p.tenant_id = 1 AND p.deleted_flag = false;

-- Credit Network Page
INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT '信用網路', 2, m.menu_id, 13, '/igaming/agent/credit', 'igaming/agent/credit/index', false, true, true, false,
    NULL, NULL, NULL, false, 1, 1, 1, NOW(), NOW()
FROM t_menu m WHERE m.menu_name = 'iGaming 管理' AND m.tenant_id = 1 AND m.deleted_flag = false;

INSERT INTO t_menu (menu_name, menu_type, parent_id, sort, path, component, frame_flag,
    cache_flag, visible_flag, disabled_flag, api_perms, perms_type, web_perms, deleted_flag,
    create_user_id, update_user_id, tenant_id, create_time, update_time)
SELECT v.menu_name, 3, p.menu_id, v.sort, NULL, NULL, false, false, true, false,
    v.api_perms, 1, v.api_perms, false, 1, 1, 1, NOW(), NOW()
FROM t_menu p
CROSS JOIN (VALUES
    ('信用查詢', 1, 'agent:credit:query'),
    ('信用分配', 2, 'agent:credit:allocate'),
    ('信用回收', 3, 'agent:credit:recall'),
    ('結算觸發', 4, 'agent:settlement:trigger'),
    ('結算驗證', 5, 'agent:settlement:verify')
) AS v(menu_name, sort, api_perms)
WHERE p.menu_name = '信用網路' AND p.tenant_id = 1 AND p.deleted_flag = false;
