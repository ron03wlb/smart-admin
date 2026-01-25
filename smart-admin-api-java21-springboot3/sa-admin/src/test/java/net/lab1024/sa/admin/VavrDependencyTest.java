package net.lab1024.sa.admin;

import static org.junit.jupiter.api.Assertions.*;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.jupiter.api.Test;

/**
 * Vavr Dependency Verification Test
 *
 * <p>This test verifies that Vavr library is correctly configured as a dependency and all core Vavr
 * types are available for use in the SmartAdmin project.
 *
 * @author Claude Code Assistant
 * @since 2026-01-25
 */
public class VavrDependencyTest {

  @Test
  void testVavrOptionAvailable() {
    // Verify Option type is available and works correctly
    Option<String> some = Option.of("test");
    Option<String> none = Option.none();

    assertTrue(some.isDefined());
    assertFalse(none.isDefined());
    assertEquals("test", some.get());
  }

  @Test
  void testVavrTryAvailable() {
    // Verify Try type is available and works correctly
    Try<Integer> success = Try.of(() -> 42);
    Try<Integer> failure =
        Try.of(
            () -> {
              throw new RuntimeException("Expected failure");
            });

    assertTrue(success.isSuccess());
    assertTrue(failure.isFailure());
    assertEquals(42, success.get());
  }

  @Test
  void testVavrEitherAvailable() {
    // Verify Either type is available and works correctly
    Either<String, Integer> right = Either.right(42);
    Either<String, Integer> left = Either.left("error");

    assertTrue(right.isRight());
    assertTrue(left.isLeft());
    assertEquals(42, right.get());
    assertEquals("error", left.getLeft());
  }
}
