# ArchUnit Test Troubleshooting

## Common Issues

### Issue 1: Compilation Failure
**Symptom**: cannot find symbol errors
**Fix**: Add ArchUnit imports

### Issue 2: Test Catches Too Much
**Symptom**: Framework classes caught
**Fix**: Use specific package patterns (net.lab1024.sa..)

### Issue 3: Test Passes But Violation Exists
**Symptom**: Missing violations
**Fix**: Check decision matrix for correct DSL pattern

### Issue 4: Test Fails on Framework Classes
**Symptom**: Spring/MyBatis violations
**Fix**: Add exemptions with .ignoreDependency()

### Issue 5: Missing .because() Clause
**Symptom**: No explanation
**Fix**: Always add .because() with rule file reference

## Debug Commands

# Compile only
./gradlew :smartadmin-app:compileTestJava

# Run specific test
./gradlew :smartadmin-app:test --tests ArchitectureTest#yourTest

# Verbose output
./gradlew :smartadmin-app:test --tests ArchitectureTest --info

## Verification Workflow

1. Write test
2. Compile
3. Run test (should pass)
4. Create intentional violation
5. Run test (should fail)
6. Remove violation
7. Run test (should pass again)

See SKILL.md for detailed troubleshooting.
