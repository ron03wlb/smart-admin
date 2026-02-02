# Mermaid 'end note' 錯誤檢測報告（修正前）

**檢測時間**: 2026-02-02 16:16
**錯誤類型**: stateDiagram-v2 中使用 `end note` 語法錯誤

---

## 📊 統計摘要

**受影響文件數**: 10
**錯誤總數**: 39

---

## 📁 受影響文件清單

### `docs/IGaming/01_Player_Center/01-02_VIP_&_Loyalty_System.md` - **3 處錯誤**

107-        降級後仍保留 50% 特權
108:    end note
109-
--
115-        - 可提交申訴
116:    end note
117-
--
122-        3. 第三次: 正式降級 (附補償)
123:    end note
124-```

---

### `docs/IGaming/02_Finance_Center/02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md` - **2 處錯誤**

725-        Reason: 等待GP確認
726:    end note
727-
--
734-        Reason: 賽果未出
735:    end note
736-

---

### `docs/IGaming/02_Finance_Center/02-04-diagrams/02-04-02_Calculation_Logic.md` - **2 處錯誤**

134-        - WALLET_DEPOSIT
135:    end note
136-    
--
139-        lockAmount = max(0, lockAmount - effectiveStake)
140:    end note
141-```

---

### `docs/IGaming/02_Finance_Center/02-07_Transaction_Processing_Flow.md` - **4 處錯誤**

287-        frozen_credit: +50
288:    end note
289-
--
295-        version += 1
296:    end note
297-
--
302-        frozen resources released
303:    end note
304-
--
309-        AND expires_at < NOW()
310:    end note
311-```

---

### `docs/IGaming/03_Game_Center/03-03_Seamless_Wallet_Analysis.md` - **2 處錯誤**

81-        - Query GP API for final status
82:    end note
83-
--
88-        - SLA: 24 hours response
89:    end note
90-```

---

### `docs/IGaming/04_Activity_Center/04-01_Activity_System_Architecture.md` - **7 處錯誤**

337-        典型時長: < 5 秒
338:    end note
339-
--
345-        (某些活動需手動激活)
346:    end note
347-
--
358-        • 高頻玩家: 1-3 天
359:    end note
360-
--
367-        • 最終可提現金額
368:    end note
369-
--
375-        此時才算 "真正獲利"
376:    end note
377-
--
388-        • 調整 wager_multiplier
389:    end note
390-
--
401-        • 7 天內必須回覆
402:    end note
403-```

---

### `docs/IGaming/04_Activity_Center/04-01_Activity_System_Design.md` - **7 處錯誤**

318-        典型時長: < 5 秒
319:    end note
320-
--
326-        (某些活動需手動激活)
327:    end note
328-
--
339-        • 高頻玩家: 1-3 天
340:    end note
341-
--
348-        • 最終可提現金額
349:    end note
350-
--
356-        此時才算 "真正獲利"
357:    end note
358-
--
369-        • 調整 wager_multiplier
370:    end note
371-
--
382-        • 7 天內必須回覆
383:    end note
384-```

---

### `docs/IGaming/04_Activity_Center/archive/04-01_Activity_System_Design_v1.0.0.md` - **7 處錯誤**

318-        典型時長: < 5 秒
319:    end note
320-
--
326-        (某些活動需手動激活)
327:    end note
328-
--
339-        • 高頻玩家: 1-3 天
340:    end note
341-
--
348-        • 最終可提現金額
349:    end note
350-
--
356-        此時才算 "真正獲利"
357:    end note
358-
--
369-        • 調整 wager_multiplier
370:    end note
371-
--
382-        • 7 天內必須回覆
383:    end note
384-```

---

### `docs/IGaming/12_Technical_Operations/12-03_Gateway_Architecture.md` - **3 處錯誤**

444-        - 滑動窗口: 最近 10 個請求
445:    end note
446-
--
453-        - 固定等待: 30 秒
454:    end note
455-
--
461-        - 失敗 → OPEN (重新跳閘)
462:    end note
463-```

---

### `docs/IGaming/archive/analysis/turnover_calculation_logic.md` - **2 處錯誤**

134-        - WALLET_DEPOSIT
135:    end note
136-    
--
139-        lockAmount = max(0, lockAmount - effectiveStake)
140:    end note
141-```

---

