package com.nowrongdoor.matching.normalize;

import com.nowrongdoor.matching.NameKey;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/**
 * Normalizes REST ({@code first_name} + {@code last_name}) and XML ({@code Name})
 * into a comparable given/surname pair.
 * <p>
 * XML official format is surname-first: {@code "KESSLER, Ashley"}.
 * Tokens are never sorted; letters are not stripped.
 */
public class NameNormalizer {

    public Optional<NameKey> fromRest(String firstName, String lastName) {
        String given = normalizeToken(firstName);
        String surname = normalizeToken(lastName);
        if (given.isEmpty() || surname.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NameKey(given, surname));
    }

    /**
     * Parse XML {@code Name}. Official records use {@code SURNAME, Given}.
     * A missing comma is treated as unparseable so it cannot silently match REST names.
     */
    public Optional<NameKey> fromXml(String xmlName) {
        if (xmlName == null) {
            return Optional.empty();
        }
        String raw = collapseWhitespace(nfc(xmlName)).trim();
        int comma = raw.indexOf(',');
        if (comma < 0) {
            return Optional.empty();
        }
        String surname = normalizeToken(raw.substring(0, comma));
        String given = normalizeToken(raw.substring(comma + 1));
        if (given.isEmpty() || surname.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NameKey(given, surname));
    }

    public String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = nfc(value).toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(codePoint -> {
            if (Character.isLetterOrDigit(codePoint) || Character.isWhitespace(codePoint)) {
                result.appendCodePoint(codePoint);
            } else {
                result.append(' ');
            }
        });
        return collapseWhitespace(result.toString()).trim();
    }

    private static String nfc(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC);
    }

    private static String collapseWhitespace(String value) {
        return value.replaceAll("\\s+", " ");
    }
}
