---
name: security-hardening-pro
description: Implement security best practices (SM2/SM3/SM4, data masking, XSS/CSRF)
priority: P0
category: backend
---

# Security Hardening Pro

Implement comprehensive security best practices including encryption (SM2/SM3/SM4), data masking, XSS/CSRF protection, and audit logging.

## Usage

```
User: "Add SM4 encryption to user passwords"
AI: [Generates encryption utility and integration code]
```

## When to Use

- Implementing encryption or data protection
- Adding XSS/CSRF protection
- Need audit logging for sensitive operations
- Implementing data masking (phone, ID card)
- Securing API endpoints

## Security Features

### Encryption (SM Series)
- **SM2**: Asymmetric encryption (public/private key)
- **SM3**: Hash function (like SHA-256)
- **SM4**: Symmetric encryption (like AES)

### Data Protection
- Phone masking: `138****1234`
- ID card masking: `310***********1234`
- Email masking: `a***@example.com`

### Web Security
- XSS prevention
- CSRF protection
- SQL injection prevention
- Security headers

## Workflow

1. Analyze security requirements
2. Generate encryption utilities
3. Implement data masking annotations
4. Add XSS/CSRF filters
5. Configure audit logging
6. Generate security tests

## Related Rules

- [S01-owasp-top10-part1.md](../../../rules/security/S01-owasp-top10-part1.md)
- [S02-owasp-top10-part2.md](../../../rules/security/S02-owasp-top10-part2.md)

## Example Session

**User:** Add data masking to user phone numbers

**AI Agent Actions:**
1. Create `@DataMask` annotation
2. Create `DataMaskSerializer` for Jackson
3. Apply to VO phone fields
4. Test serialization output
