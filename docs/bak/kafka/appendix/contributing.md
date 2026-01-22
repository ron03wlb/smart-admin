# Contributing Guide

Guide for contributing to SmartAdmin Kafka integration.

## Welcome!

Thank you for your interest in contributing to SmartAdmin Kafka integration! This guide will help you get started with contributing code, documentation, bug reports, or feature requests.

**Ways to contribute**:
- Report bugs and issues
- Suggest new features or improvements
- Submit code changes (bug fixes, features)
- Improve documentation
- Write examples and tutorials

---

## Getting Started

### Prerequisites

**Required**:
- Java 21 JDK
- Gradle 8.x
- Docker (for running Kafka locally)
- Git

**Recommended**:
- IntelliJ IDEA or similar IDE
- Kafka CLI tools
- Knowledge of Spring Boot and Spring Kafka

### Setting Up Development Environment

**1. Fork and clone**:
```bash
# Fork repository on GitHub first
git clone https://github.com/YOUR_USERNAME/smart-admin.git
cd smart-admin

# Add upstream remote
git remote add upstream https://github.com/1024-lab/smart-admin.git
```

**2. Build project**:
```bash
./gradlew clean build
```

**3. Run tests**:
```bash
./gradlew :sa-admin:test
./gradlew :sa-common:sa-common-module-mq-kafka:test
```

**4. Start Kafka locally**:
```bash
cd docker
docker-compose -f docker-compose-kafka.yml up -d
```

**5. Run application**:
```bash
./gradlew :sa-admin:bootRun
```

---

## Code Contributions

### Before You Start

**1. Check existing issues**:
- Search GitHub issues to avoid duplicate work
- Comment on issue if you want to work on it

**2. Discuss major changes**:
- For significant features, open an issue first
- Get feedback before investing time

**3. Keep changes focused**:
- One feature/fix per pull request
- Small, reviewable changes are better

### Development Workflow

**1. Create a branch**:
```bash
git checkout -b feature/kafka-custom-serializer
# or
git checkout -b fix/kafka-consumer-lag
```

**Branch naming**:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `test/` - Test improvements

**2. Make your changes**:
```bash
# Edit code
vim sa-common/sa-common-module-mq-kafka/src/main/java/...

# Run tests frequently
./gradlew :sa-common:sa-common-module-mq-kafka:test

# Format code (if using IntelliJ)
Code → Reformat Code (Ctrl+Alt+L)
```

**3. Write tests**:
```java
@SpringBootTest
@EmbeddedKafka
class CustomSerializerTest {

    @Test
    void testCustomSerialization() {
        // Arrange
        MyObject obj = new MyObject("test");

        // Act
        kafkaProducerService.send("test-topic", "key", serialize(obj));

        // Assert
        await().until(() -> messageReceived);
        assertThat(deserializedObject).isEqualTo(obj);
    }
}
```

**4. Commit your changes**:
```bash
git add .
git commit -m "feat(kafka): add custom serializer support"
```

**Commit message format** (Conventional Commits):
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, no logic change)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Build, dependencies, tooling

**Examples**:
```
feat(kafka): add batch processing support

Implement AbstractBatchKafkaListener with graceful degradation.
Improves throughput by 6x for bulk operations.

Closes #123
```

```
fix(kafka): prevent consumer lag accumulation

Increase max.poll.records to 500 and optimize processing logic.

Fixes #456
```

**5. Push and create pull request**:
```bash
git push origin feature/kafka-custom-serializer
```

Then create pull request on GitHub.

---

## Code Standards

### Architecture Rules

**1. Layered architecture** (enforced by ArchUnit):
```
Controller → Service → Manager → Dao
```

**Kafka components**:
- Producers in Service layer
- Consumers extend `AbstractKafkaListener`
- No direct `KafkaTemplate` injection outside `KafkaProducerService`

**2. Dependency injection**:
```java
// ✅ Good: Constructor injection
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaProducerService kafkaProducerService;
}

// ❌ Bad: Field injection
@Autowired
private KafkaProducerService kafkaProducerService;
```

**3. No circular dependencies**:
- Service should not depend on Controller
- Manager should not depend on Service
- Listener should only inject Service, not other Listeners

### Naming Conventions

**Classes**:
```java
OrderListener          // Consumer listener
OrderService           // Business logic
OrderManager           // Transaction/caching
OrderDao               // Database access
OrderDTO               // Data transfer object
OrderEntity            // Database entity
```

**Methods**:
```java
getUserById()          // Get single entity
listUsers()            // Get multiple entities
saveUser()             // Create or update
deleteUser()           // Delete entity
processOrder()         // Business operation
```

**Boolean fields**:
```java
private Boolean deleted;      // ✅ Good
private Boolean isDeleted;    // ❌ Bad (no "is" prefix)
```

### Code Quality

**1. No null returns**:
```java
// ✅ Good
return ResponseDTO.error(ErrorCode.NOT_FOUND);

// ❌ Bad
return null;
```

**2. Proper exception handling**:
```java
// ✅ Good
try {
    processMessage(record);
} catch (BusinessException e) {
    throw e;  // Let AbstractKafkaListener handle
}

// ❌ Bad
try {
    processMessage(record);
} catch (Exception e) {
    e.printStackTrace();  // Don't swallow exceptions
}
```

**3. Logging**:
```java
// ✅ Good: Structured logging with placeholders
log.info("Message processed | OrderId: {} | Amount: {}", orderId, amount);

// ❌ Bad: String concatenation
log.info("Message processed | OrderId: " + orderId);
```

---

## Testing Requirements

### Test Coverage

- **Minimum**: 80% line coverage for new code
- **Target**: 90% line coverage

**Check coverage**:
```bash
./gradlew :sa-common:sa-common-module-mq-kafka:test jacocoTestReport

# Open report
open sa-common/sa-common-module-mq-kafka/build/reports/jacoco/test/html/index.html
```

### Test Types

**1. Unit tests** (fast, isolated):
```java
@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void testSendMessage() {
        // Test with mocks
    }
}
```

**2. Integration tests** (with EmbeddedKafka):
```java
@SpringBootTest
@EmbeddedKafka
class OrderListenerIntegrationTest {
    @Test
    void testEndToEndFlow() {
        // Test with real Kafka
    }
}
```

**3. Architecture tests** (ArchUnit):
```java
@Test
void testLayeredArchitecture() {
    classes()
        .that().resideInAPackage("..controller..")
        .should().onlyAccessClassesThat().resideInAnyPackage("..service..", "..domain..")
        .check(importedClasses);
}
```

---

## Documentation Contributions

### Writing Documentation

**Location**: `docs/kafka/`

**Structure**:
```
docs/kafka/
├── getting-started/    # Quick start, references
├── architecture/       # Design docs
├── guides/             # How-to guides
├── operations/         # Deployment, monitoring
├── examples/           # Complete examples
├── testing/            # Testing guides
├── reference/          # API, config reference
└── appendix/           # Glossary, changelog
```

**Document format**:
```markdown
# Title

Brief description.

## Overview

Context and purpose.

## Section 1

Content with code examples.

\`\`\`java
// Code example
\`\`\`

## See Also

- [Related Doc 1](/path/to/doc1)
- [Related Doc 2](/path/to/doc2)

---

**Last Updated**: YYYY-MM-DD
```

**Guidelines**:
- Use clear, concise language
- Include code examples for concepts
- Add diagrams where helpful (Mermaid)
- Cross-reference related documents
- Keep examples copy-paste ready
- Update "Last Updated" date

### Building Documentation Site

```bash
# Install VitePress
npm install -D vitepress

# Dev server
npm run docs:dev

# Build for production
npm run docs:build

# Preview production build
npm run docs:preview
```

---

## Pull Request Process

### PR Checklist

Before submitting PR, ensure:

- [ ] Code follows SmartAdmin coding standards
- [ ] All tests pass locally
- [ ] New tests added for new functionality
- [ ] Test coverage ≥ 80%
- [ ] Documentation updated (if needed)
- [ ] Commit messages follow Conventional Commits
- [ ] No merge conflicts with master
- [ ] PR description explains changes

### PR Template

```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project standards
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes (or documented)

## Related Issues
Closes #123
Fixes #456
```

### Review Process

**1. Automated checks**:
- Build passes (Gradle)
- Tests pass (JUnit, ArchUnit)
- Code style (Checkstyle)
- Coverage meets threshold (JaCoCo)

**2. Manual review**:
- Code quality and design
- Test adequacy
- Documentation completeness
- Breaking changes identified

**3. Addressing feedback**:
```bash
# Make requested changes
git add .
git commit -m "fix: address review feedback"
git push origin feature/my-feature
```

**4. Merge**:
- Squash commits (default)
- Update changelog
- Create GitHub release (for versions)

---

## Reporting Issues

### Bug Reports

**Before reporting**:
- Search existing issues
- Verify bug on latest version
- Reproduce in minimal environment

**Bug report template**:
```markdown
## Description
Clear description of the bug.

## To Reproduce
Steps to reproduce:
1. Start application with...
2. Send message to...
3. Observe error...

## Expected Behavior
What should happen instead.

## Actual Behavior
What actually happened.

## Environment
- SmartAdmin version: 3.5.4
- Java version: 21
- Kafka version: 3.5.0
- OS: macOS 14.0

## Logs
\`\`\`
Paste relevant logs here
\`\`\`

## Additional Context
Screenshots, config files, etc.
```

### Feature Requests

**Feature request template**:
```markdown
## Problem
Describe the problem this feature would solve.

## Proposed Solution
Describe your proposed solution.

## Alternatives Considered
Other approaches you considered.

## Use Case
Example of how this would be used.

## Additional Context
Any other relevant information.
```

---

## Community Guidelines

### Code of Conduct

**Be respectful**:
- Welcome newcomers
- Be patient with questions
- Provide constructive feedback
- Assume good intentions

**Be collaborative**:
- Share knowledge
- Help others learn
- Review PRs
- Answer questions

### Getting Help

**Resources**:
- Documentation: [SmartAdmin Kafka Docs](/kafka/)
- Issues: [GitHub Issues](https://github.com/1024-lab/smart-admin/issues)
- Discussions: [GitHub Discussions](https://github.com/1024-lab/smart-admin/discussions)

**Asking questions**:
- Search documentation first
- Provide context and details
- Include code examples
- Share error messages/logs

---

## Recognition

Contributors will be:
- Listed in CONTRIBUTORS.md
- Mentioned in release notes
- Credited in commit history

Thank you for contributing to SmartAdmin! 🎉

---

## See Also

- [Architecture Rules](.agent/rules/10-architecture-rules.md) - Detailed architecture guidelines
- [Naming Conventions](.agent/rules/01-naming-conventions.md) - Naming standards
- [Testing Strategy](/kafka/testing/testing-strategy) - Testing approach

---

**Last Updated**: 2026-01-22
