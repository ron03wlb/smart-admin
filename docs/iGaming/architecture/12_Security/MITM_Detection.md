# MITM Detection Architecture

> **Business Requirements**: [Payment Security Requirements](../../requirements/12_Security_Compliance/Payment_Security_Requirements.md)
> **Canonical Source**: [source-archive/12_System_Security/12-08](../../source-archive/12_System_Security/12-08_MITM_Detection.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Security Engineers, Backend Developers

---

## 1. Detection Capabilities

| Capability | Description | Detection Latency |
|-----------|-------------|-------------------|
| **TLS Downgrade Detection** | Detect forced TLS version downgrade | Real-time |
| **Certificate Pinning** | Verify server certificate legitimacy | Real-time |
| **Session Hijack Detection** | Identify stolen session tokens | Near real-time (<1s) |
| **DNS Spoofing Detection** | Detect DNS resolution anomalies | Real-time |
| **Proxy Injection Detection** | Identify malicious proxy interception | Real-time |

## 2. TLS Security Configuration

```java
@Configuration
public class TLSSecurityConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tlsCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            if (connector.getProtocolHandler() instanceof Http11NioProtocol) {
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

                // Allow only TLS 1.3 and TLS 1.2
                protocol.setSSLProtocol("TLSv1.3,TLSv1.2");

                // Disable weak cipher suites
                protocol.setCiphers(
                    "TLS_AES_256_GCM_SHA384," +
                    "TLS_CHACHA20_POLY1305_SHA256," +
                    "TLS_AES_128_GCM_SHA256," +
                    "ECDHE-RSA-AES256-GCM-SHA384," +
                    "ECDHE-RSA-AES128-GCM-SHA256"
                );

                // Disable SSL compression (prevent CRIME attack)
                protocol.setCompression("off");
            }
        });
    }
}
```

## 3. Certificate Pinning Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificatePinningService {

    private static final Set<String> PINNED_HASHES = Set.of(
        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // Primary cert
        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="  // Backup cert
    );

    public boolean verifyCertificatePin(X509Certificate certificate) {
        try {
            byte[] publicKeyBytes = certificate.getPublicKey().getEncoded();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKeyBytes);
            String base64Hash = "sha256/" + Base64.getEncoder().encodeToString(hash);

            boolean valid = PINNED_HASHES.contains(base64Hash);

            if (!valid) {
                log.warn("Certificate pinning validation failed. Hash: {}", base64Hash);
                securityEventService.logCertificatePinningFailure(base64Hash);
            }

            return valid;
        } catch (NoSuchAlgorithmException e) {
            log.error("Certificate pinning verification error", e);
            return false;
        }
    }
}
```

## 4. Session Binding Detector

```java
@Service
@RequiredArgsConstructor
public class SessionBindingDetector {

    public SessionBindingResult validateSessionBinding(
            String sessionId,
            String currentFingerprint,
            String currentIp) {

        SessionBinding binding = sessionBindingDao.findBySessionId(sessionId);

        if (binding == null) {
            createSessionBinding(sessionId, currentFingerprint, currentIp);
            return SessionBindingResult.valid();
        }

        List<String> violations = new ArrayList<>();

        // 1. Device fingerprint check
        if (!binding.getDeviceFingerprint().equals(currentFingerprint)) {
            violations.add("DEVICE_MISMATCH");
        }

        // 2. IP subnet check
        if (!isSameSubnet(binding.getIpAddress(), currentIp)) {
            if (isImpossibleTravel(binding.getLastActiveAt(), binding.getIpAddress(), currentIp)) {
                violations.add("IMPOSSIBLE_TRAVEL");
            } else {
                violations.add("IP_CHANGE");
            }
        }

        if (!violations.isEmpty()) {
            return SessionBindingResult.builder()
                .valid(false)
                .violations(violations)
                .riskLevel(determineRiskLevel(violations))
                .build();
        }

        return SessionBindingResult.valid();
    }

    private RiskLevel determineRiskLevel(List<String> violations) {
        if (violations.contains("DEVICE_MISMATCH") && violations.contains("IMPOSSIBLE_TRAVEL")) {
            return RiskLevel.CRITICAL; // Likely MITM attack
        }
        if (violations.contains("DEVICE_MISMATCH")) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.MEDIUM;
    }
}
```

## 5. Proxy Detection Service

```java
@Service
@RequiredArgsConstructor
public class ProxyDetectionService {

    public ProxyDetectionResult detectProxy(HttpServletRequest request) {
        List<String> signals = new ArrayList<>();

        // 1. Check proxy-related headers
        String[] proxyHeaders = {
            "X-Forwarded-For", "Via", "Proxy-Connection",
            "X-Real-IP", "Forwarded"
        };

        for (String header : proxyHeaders) {
            if (StringUtils.hasText(request.getHeader(header))) {
                signals.add("PROXY_HEADER:" + header);
            }
        }

        // 2. Check known proxy/VPN IPs
        String clientIp = getClientIp(request);
        if (knownProxyService.isKnownProxy(clientIp)) {
            signals.add("KNOWN_PROXY_IP");
        }

        // 3. Check datacenter IPs
        if (ipInfoService.isDatacenterIp(clientIp)) {
            signals.add("DATACENTER_IP");
        }

        // 4. Timezone vs IP geolocation mismatch
        String browserTimezone = request.getHeader("X-Timezone");
        String ipTimezone = geoService.getTimezone(clientIp);
        if (browserTimezone != null && !browserTimezone.equals(ipTimezone)) {
            signals.add("TIMEZONE_MISMATCH");
        }

        return ProxyDetectionResult.builder()
            .proxyDetected(!signals.isEmpty())
            .signals(signals)
            .riskLevel(calculateRiskLevel(signals))
            .build();
    }
}
```

## 6. Real-Time Monitoring Flow

```mermaid
flowchart TD
    A[Inbound Request] --> B[TLS Handshake Analysis]

    B --> C{TLS Version Check}
    C -->|< TLS 1.2| D[Block + Alert]
    C -->|>= TLS 1.2| E[Certificate Validation]

    E --> F{Certificate Pinning}
    F -->|Failed| G[Block + Security Event]
    F -->|Passed| H[Session Binding Check]

    H --> I{Device/IP Consistent?}
    I -->|Mismatch| J[Step-Up Authentication]
    I -->|Match| K[Normal Processing]

    J --> L{Auth Success?}
    L -->|Yes| K
    L -->|No| M[Terminate Session]
```

## 7. Alert Rules

| Alert Type | Trigger | Severity | Action |
|-----------|---------|----------|--------|
| **TLS Downgrade** | TLS 1.0/1.1 detected | Critical | Block + Record |
| **Certificate Pin Fail** | Cert not in pin list | Critical | Block + Investigate |
| **Session Hijack** | Device + IP both changed | Critical | Terminate session |
| **Suspicious Proxy** | Multiple proxy signals | High | Step-Up auth |
| **DNS Anomaly** | Resolution inconsistency | High | Verify + Record |

## 8. Database Schema

```sql
CREATE TABLE t_mitm_detection_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(50) NOT NULL UNIQUE,
    session_id VARCHAR(100),
    player_id BIGINT,
    client_ip VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    detection_type ENUM(
        'TLS_DOWNGRADE', 'CERTIFICATE_PIN_FAILURE',
        'SESSION_HIJACK', 'PROXY_DETECTED', 'DNS_SPOOFING'
    ) NOT NULL,
    detection_signals JSON,
    risk_score INT NOT NULL,
    risk_level ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    action_taken ENUM('LOG', 'STEP_UP', 'BLOCK', 'TERMINATE_SESSION') NOT NULL,
    action_result ENUM('SUCCESS', 'FAILURE'),
    tls_version VARCHAR(20),
    cipher_suite VARCHAR(100),
    detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_player (player_id, detected_at),
    INDEX idx_detection_type (detection_type, detected_at),
    INDEX idx_risk_level (risk_level, detected_at)
) ENGINE=InnoDB;

CREATE TABLE t_session_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    player_id BIGINT NOT NULL,
    device_fingerprint VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    ip_subnet VARCHAR(45),
    geo_country VARCHAR(2),
    geo_city VARCHAR(100),
    request_count INT DEFAULT 1,
    last_active_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,

    INDEX idx_player (player_id),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB;
```

## 9. Prometheus Metrics

```yaml
mitm_detection_total:
  type: counter
  labels: [detection_type, action_taken, risk_level]
  description: "MITM detection event count"

tls_handshake_version:
  type: counter
  labels: [version]
  description: "TLS handshake version distribution"

session_binding_violation:
  type: counter
  labels: [violation_type]
  description: "Session binding violation stats"

certificate_pinning_result:
  type: counter
  labels: [result]
  description: "Certificate pinning verification result"
```

## 10. Key KPIs

| Metric | Formula | Target | Alert |
|--------|---------|--------|-------|
| TLS 1.3 Adoption | TLS 1.3 requests / total | > 90% | < 80% |
| Certificate Pin Failure Rate | Failures / total verifications | < 0.01% | > 0.1% |
| Session Hijack Detections | Daily CRITICAL events | < 5 | > 20 |
| Proxy Detection Rate | Proxy requests / total | Monitor | Abnormal increase |
