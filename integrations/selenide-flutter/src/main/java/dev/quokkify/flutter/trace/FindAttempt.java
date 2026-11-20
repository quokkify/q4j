package dev.quokkify.flutter.trace;

import java.time.Duration;
import java.time.Instant;

import org.openqa.selenium.By;

public record FindAttempt(By by, String locator, Instant start, Duration duration, Outcome outcome, String detail) {
}
