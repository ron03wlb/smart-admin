package net.lab1024.sa.foundation.ipgeo.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.domain.constant.StringConst;
import net.lab1024.sa.util.SmartStringUtil;
import org.lionsoul.ip2region.xdb.Searcher;

/**
 * IP工具类
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/9/14 15:35:11 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Slf4j
public class IpGeolocationUtil {

  private static final AtomicReference<Searcher> IP_SEARCHER = new AtomicReference<>();

  /**
   * 初始化数据
   *
   * @param filePath
   */
  public static void init(String filePath) {

    try {
      byte[] cBuff = Searcher.loadContentFromFile(filePath);
      IP_SEARCHER.set(Searcher.newWithBuffer(cBuff));

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("初始化ip2region.xdb文件失败,报错信息:[{}]", e.getMessage(), e);
      }
      throw new RuntimeException("系统异常!", e);
    } catch (Error e) {
      if (log.isErrorEnabled()) {
        log.error("初始化ip2region.xdb文件失败,报错信息:[{}]", e.getMessage(), e);
      }
      throw e;
    }
  }

  /**
   * 自定义解析ip地址
   *
   * <p>注意：此方法吞沒異常並返回空列表，以避免 IP 解析失敗影響業務流程
   *
   * @param ipStr ipStr
   * @return 返回结果例 [河南省, 洛阳市, 洛龙区]，解析失敗返回空列表
   */
  public static List<String> getRegionList(String ipStr) {
    List<String> regionList = new ArrayList<>();
    try {
      if (SmartStringUtil.isEmpty(ipStr)) {
        return regionList;
      }
      String finalIpStr = ipStr.trim();
      String region = IP_SEARCHER.get().search(finalIpStr);
      String[] split = region.split("\\|");
      regionList.addAll(Arrays.asList(split));
    } catch (Exception e) {
      log.warn("解析IP地址失敗 - ip: {}, 返回空列表", ipStr, e);
    }
    return regionList;
  }

  /**
   * 自定义解析ip地址
   *
   * <p>注意：此方法吞沒異常並返回空字符串，以避免 IP 解析失敗影響業務流程
   *
   * @param ipStr ipStr
   * @return 返回结果例 河南省|洛阳市|洛龙区，解析失敗返回空字符串
   */
  public static String getRegion(String ipStr) {
    try {
      if (SmartStringUtil.isEmpty(ipStr)) {
        return StringConst.EMPTY;
      }
      String finalIpStr = ipStr.trim();
      return IP_SEARCHER.get().search(finalIpStr);
    } catch (Exception e) {
      log.warn("解析IP地址失敗 - ip: {}, 返回空字符串", ipStr, e);
      return StringConst.EMPTY;
    }
  }

  /**
   * 获取本机第一个ip
   *
   * @return
   */
  public static String getLocalFirstIp() {
    List<String> list = getLocalIp();
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * 获取本机ip
   *
   * @return
   */
  public static List<String> getLocalIp() {
    List<String> ipList = new ArrayList<>();
    try {
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
          InetAddress inetAddress = inetAddresses.nextElement();
          // 排除回环地址和IPv6地址
          if (!inetAddress.isLoopbackAddress()
              && !inetAddress.getHostAddress().contains(StringConst.COLON)) {
            ipList.add(inetAddress.getHostAddress());
          }
        }
      }
    } catch (SocketException e) {
      log.error("获取本机IP出错", e);
    }
    return ipList;
  }
}
