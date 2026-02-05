# iGame Feature Builder - Examples

## 範例: VIP 升級邏輯

```java
@Service
@RequiredArgsConstructor
public class VipService {

    private final VipDao vipDao;
    private final VipManager vipManager;

    public ResponseDTO<VipVO> checkAndUpgrade(Long userId) {
        VipEntity vip = vipDao.getByUserId(userId);
        int currentPoints = vip.getTotalPoints();

        VipTier newTier = VipTier.getTierByPoints(currentPoints);

        if (newTier.getLevel() > vip.getTier().getLevel()) {
            return vipManager.upgradeTier(userId, newTier);
        }

        return ResponseDTO.ok(SmartBeanUtil.copy(vip, VipVO.class));
    }
}
```
