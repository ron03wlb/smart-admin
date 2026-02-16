package net.lab1024.sa.common.web.web.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.lab1024.sa.common.core.tenant.TenantContext;

/**
 * Jackson serializer that converts OffsetDateTime (UTC) to the tenant's timezone.
 *
 * <p>Output format: ISO-8601 with offset, e.g. "2026-02-15T18:30:00+08:00"
 *
 * <p>When TenantContext has no timezone set (e.g. background jobs), falls back to UTC.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
public class TenantTimezoneSerializer extends JsonSerializer<OffsetDateTime> {

  @Override
  public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    if (value == null) {
      gen.writeNull();
      return;
    }
    ZoneId tenantZone = TenantContext.getZoneId();
    OffsetDateTime tenantTime = value.atZoneSameInstant(tenantZone).toOffsetDateTime();
    gen.writeString(tenantTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
  }

  @Override
  public Class<OffsetDateTime> handledType() {
    return OffsetDateTime.class;
  }
}
