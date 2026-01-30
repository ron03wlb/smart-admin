# Root Layer Archive (v3.0.0)

已於 v4.0.0 遷移至分層結構。

## 遷移信息

- **遷移映射表**: `../../scripts/root-to-hierarchical-mapping.json`
- **遷移日期**: 2026-01-29
- **保留期限**: 2026-07-29 (6 個月後刪除)
- **備份原因**: v4.0.0 根層清理，所有技能已遷移至分層結構

## 還原方法

如需還原任何技能：

```bash
cp -r _deprecated/root-layer-v3/{skill-name} .claude/skills/
```

## 警告

⚠️ 這些是舊版本的重複副本。正式版本已在分層結構中：
- foundation/
- extended/
- productivity/
- lifecycle/

請勿直接使用此備份中的文件。
