package com.nowrongdoor.matching.normalize;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Canonicalizes source dates. Blank/null is missing. {@code YYYY-MM-DD} is kept as-is.
 * No day/month swapping and no inference of missing values.
 */
public class DateNormalizer {

    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    public boolean isMissing(String raw) {
        return raw == null || raw.isBlank();
    }

    /**
     * @return canonical {@code YYYY-MM-DD} when the value is an ISO date;
     *         empty when missing. Non-ISO non-blank values are not guessed.
     */
    public Optional<String> canonicalize(String raw) {
        if (isMissing(raw)) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (ISO_DATE.matcher(trimmed).matches()) {
            try {
                return Optional.of(LocalDate.parse(trimmed).toString());
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** True only when both sides have a canonical date and they are equal. */
    public boolean bothPresentAndEqual(String left, String right) {
        Optional<String> a = canonicalize(left);
        Optional<String> b = canonicalize(right);
        return a.isPresent() && b.isPresent() && a.get().equals(b.get());
    }

    /** True when both sides have a canonical date and they differ. */
    public boolean bothPresentAndConflict(String left, String right) {
        Optional<String> a = canonicalize(left);
        Optional<String> b = canonicalize(right);
        return a.isPresent() && b.isPresent() && !a.get().equals(b.get());
    }
}
