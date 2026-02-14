package net.lab1024.sa.support.serialnumber.service;

import java.util.List;
import net.lab1024.sa.support.serialnumber.constant.SerialNumberIdEnum;

/**
 * 单据序列号
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressWarnings("PMD.LongVariable")
public interface SerialNumberService {

  /**
   * 生成
   *
   * @param serialNumberIdEnum
   * @return
   */
  String generate(SerialNumberIdEnum serialNumberIdEnum);

  /**
   * 生成n个
   *
   * @param serialNumberIdEnum
   * @param count
   * @return
   */
  List<String> generate(SerialNumberIdEnum serialNumberIdEnum, int count);
}
