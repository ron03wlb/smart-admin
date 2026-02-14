# Security Hardening Pro - Quick Reference

**Version**: 1.0.0  
**Last Updated**: 2026-02-02  
**Skill**: security-hardening-pro (P0 - Critical)

---

## Quick Commands

### Enable API Encryption

@ApiEncrypt  // On controller method
ResponseDTO<String> withdraw(@RequestBody @Valid WithdrawalForm form)

### Configure SM2/SM3/SM4

sa:
  api-encrypt:
    enabled: true
    algorithm: SM4  # or SM2, SM3
    key: ${API_ENCRYPT_KEY}

### Data Masking

@DataMasking(type = DataMaskType.PHONE)
private String phone;

@DataMasking(type = DataMaskType.ID_CARD)
private String idCard;

### XSS Protection

# Automatic with SmartAdmin XssFilter (already configured)
# Manual sanitization:
String clean = XssUtil.stripXss(userInput);

### SQL Injection Prevention

# Use MyBatis-Plus #{} binding (automatic)
@Select("SELECT * FROM user WHERE id = #{id}")  // ✅ Safe

# NEVER use ${} for user input
@Select("SELECT * FROM user WHERE id = ${id}")   // ❌ Vulnerable

### Audit Logging

@AuditLog(module = "payment", operation = "withdrawal")
public ResponseDTO<String> withdraw(WithdrawalForm form)

---

## 5 Core Security Patterns

### 1. SM2/SM3/SM4 Encryption
- SM2: Asymmetric (public/private key)
- SM3: Hash (256-bit)
- SM4: Symmetric (AES-like)

### 2. Data Masking
- Phone: 138****5678
- Email: u***@example.com
- ID Card: 110***********1234

### 3. XSS/CSRF Protection
- XssFilter: Auto-strips scripts
- CSRF Token: Spring Security

### 4. SQL Injection Prevention
- Use MyBatis #{} binding
- Validate all user inputs

### 5. Audit Logging
- Financial operations
- User profile changes
- Permission modifications

---

## Configuration Checklist

- [ ] Enable API encryption in application.yml
- [ ] Configure encryption keys (environment variables)
- [ ] Add @DataMasking to sensitive fields
- [ ] Enable XssFilter (default in SmartAdmin)
- [ ] Use MyBatis #{} binding for SQL
- [ ] Add @AuditLog to critical operations
- [ ] Test with security scan tools

---

## Common Errors

### Error: Encryption key not found
**Fix**: Set environment variable API_ENCRYPT_KEY

### Error: Data masking not working
**Fix**: Ensure @DataMasking is on entity field, not VO

### Error: XSS still present
**Fix**: Check if XssFilter is enabled in security config

---

See SKILL.md for complete implementation guide.
