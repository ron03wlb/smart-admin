package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * KYC document type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum KycDocumentTypeEnum implements BaseEnum {
  ID_CARD(1, "身分證"),
  PASSPORT(2, "護照"),
  DRIVER_LICENSE(3, "駕照"),
  ;

  private final Integer value;
  private final String desc;
}
