package net.lab1024.sa.foundation.captcha;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import io.vavr.control.Option;
import java.awt.Color;
import java.awt.Image;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.cache.CacheService;
import net.lab1024.sa.foundation.cache.constant.CacheKeyConst;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 图形验证码服务
 *
 * <p>提供验证码的生成和校验功能，支持图形验证码的创建、存储和验证。
 *
 * <p>验证码特性：
 *
 * <ul>
 *   <li>4位数字验证码
 *   <li>125x43像素图片大小
 *   <li>80条干扰线
 *   <li>65秒过期时间
 *   <li>非生产环境返回验证码明文
 *   <li>使用Redis存储验证码
 * </ul>
 *
 * @author 1024创新实验室: 胡克
 * @since 2021/8/31 20:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

  /** 验证码过期时间：65秒 */
  private static final long EXPIRE_SECOND = 65L;

  /** 验证码图片宽度 */
  private static final int CAPTCHA_WIDTH = 125;

  /** 验证码图片高度 */
  private static final int CAPTCHA_HEIGHT = 43;

  /** 验证码位数 */
  private static final int CAPTCHA_LENGTH = 4;

  /** 干扰线数量 */
  private static final int LINE_COUNT = 80;

  /** 背景颜色 RGB(230, 244, 255) */
  private static final Color BACKGROUND_COLOR = new Color(230, 244, 255);

  /** 当前运行环境的 profile */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  private final CacheService cacheService;

  /**
   * 生成图形验证码
   *
   * <p>生成一个4位数字的验证码图片，并将验证码文本存储到Redis中。 非生产环境会在返回的VO中包含验证码明文，方便开发调试。
   *
   * @return CaptchaVO 包含验证码UUID、Base64图片和过期时间
   */
  public CaptchaVO generateCaptcha() {

    // 生成四位验证码
    String captchaText = RandomUtil.randomNumbers(CAPTCHA_LENGTH);

    // 定义图形验证码的长、宽、验证码位数、干扰线数量
    LineCaptcha lineCaptcha =
        CaptchaUtil.createLineCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_LENGTH, LINE_COUNT);

    // 设置背景颜色
    lineCaptcha.setBackground(BACKGROUND_COLOR);

    // 生成图片
    Image image = lineCaptcha.createImage(captchaText);

    // 转为base64
    String base64Code = ImgUtil.toBase64(image, "jpg");

    // uuid 唯一标识
    String uuid = IdUtil.fastSimpleUUID();

    CaptchaVO captchaVO = new CaptchaVO();
    captchaVO.setCaptchaUuid(uuid);
    captchaVO.setCaptchaBase64Image("data:image/png;base64," + base64Code);
    captchaVO.setExpireSeconds(EXPIRE_SECOND);

    // 非生产环境返回验证码明文，方便测试
    if (!isProd()) {
      captchaVO.setCaptchaText(captchaText);
    }

    // 将验证码存入缓存
    cacheService.put(
        CacheKeyConst.Support.CAPTCHA, uuid, captchaText, EXPIRE_SECOND, TimeUnit.SECONDS);

    return captchaVO;
  }

  /**
   * 校验图形验证码
   *
   * <p>验证用户输入的验证码是否正确，验证成功后会自动删除Redis中的验证码。
   *
   * @param captchaForm 验证码表单，包含验证码和UUID
   * @throws CaptchaException 当验证码为空、过期或错误时抛出
   */
  public void checkCaptcha(CaptchaForm captchaForm) {
    Objects.requireNonNull(captchaForm, "验证码表单不能为空");

    if (StringUtils.isBlank(captchaForm.getCaptchaUuid())
        || StringUtils.isBlank(captchaForm.getCaptchaCode())) {
      throw new CaptchaException("请输入正确验证码");
    }

    // 从Redis获取验证码
    Option<String> captchaOpt =
        cacheService.get(CacheKeyConst.Support.CAPTCHA, captchaForm.getCaptchaUuid(), String.class);
    String redisCaptchaCode = captchaOpt.getOrNull();

    if (StringUtils.isBlank(redisCaptchaCode)) {
      throw new CaptchaException("验证码已过期，请刷新重试");
    }

    if (!Objects.equals(redisCaptchaCode, captchaForm.getCaptchaCode())) {
      throw new CaptchaException("验证码错误，请输入正确的验证码");
    }

    // 删除已使用的验证码
    cacheService.remove(CacheKeyConst.Support.CAPTCHA, captchaForm.getCaptchaUuid());
  }

  /**
   * 判断当前是否为生产环境
   *
   * @return true表示生产环境，false表示非生产环境
   */
  private boolean isProd() {
    return "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
  }
}
