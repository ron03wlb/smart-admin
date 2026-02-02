package net.lab1024.sa.base.module.support.liteflow.constant;

/**
 * LiteFlow 常量定義
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
public class LiteFlowConst {

  // ========== 流程類型 ==========
  /** 流程類型：普通流程 */
  public static final Integer CHAIN_TYPE_NORMAL = 1;

  /** 流程類型：條件流程 */
  public static final Integer CHAIN_TYPE_CONDITIONAL = 2;

  /** 流程類型：循環流程 */
  public static final Integer CHAIN_TYPE_LOOP = 3;

  // ========== 狀態 ==========
  /** 狀態：禁用 */
  public static final Integer STATUS_DISABLED = 0;

  /** 狀態：啟用 */
  public static final Integer STATUS_ENABLED = 1;

  // ========== 執行狀態 ==========
  /** 執行狀態：失敗 */
  public static final Integer EXECUTION_FAILED = 0;

  /** 執行狀態：成功 */
  public static final Integer EXECUTION_SUCCESS = 1;

  private LiteFlowConst() {
    // Utility class - prevent instantiation
  }
}
