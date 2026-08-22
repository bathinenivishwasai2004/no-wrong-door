package com.nowrongdoor.matching.normalize;

import com.nowrongdoor.matching.AddressAndPlaceKey;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Normalizes REST {@code address_line}/{@code city} and XML {@code Addr}/{@code Town}.
 * Street suffixes are expanded so {@code St} and {@code Street} compare equal.
 * House numbers are preserved. Address is never an identity key by itself.
 */
public class AddressNormalizer {

    private static final Map<String, String> SUFFIXES = Map.of(
            "st", "street",
            "street", "street",
            "rd", "road",
            "road", "road",
            "ave", "avenue",
            "avenue", "avenue",
            "dr", "drive",
            "drive", "drive",
            "ln", "lane",
            "lane", "lane"
    );

    public Optional<AddressAndPlaceKey> key(String line, String place) {
        String nLine = normalizeLine(line);
        String nPlace = normalizePlace(place);
        if (nLine.isEmpty() || nPlace.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AddressAndPlaceKey(nLine, nPlace));
    }

    public String normalizeLine(String line) {
        String prepared = stripUnsafePunctuation(normalizeBase(line));
        if (prepared.isEmpty()) {
            return "";
        }
        int lastSpace = prepared.lastIndexOf(' ');
        if (lastSpace < 0) {
            return expandSuffix(prepared);
        }
        String head = prepared.substring(0, lastSpace);
        String last = prepared.substring(lastSpace + 1);
        return head + " " + expandSuffix(last);
    }

    public String normalizePlace(String place) {
        return normalizeBase(place);
    }

    /** Original last token before expansion, for match notes (e.g. {@code St}). */
    public String originalSuffixToken(String line) {
        String prepared = stripUnsafePunctuation(normalizeBase(line));
        if (prepared.isEmpty()) {
            return "";
        }
        int lastSpace = prepared.lastIndexOf(' ');
        return lastSpace < 0 ? prepared : prepared.substring(lastSpace + 1);
    }

    public String expandedSuffixLabel(String line) {
        String token = originalSuffixToken(line);
        String expanded = expandSuffix(token);
        if (token.isEmpty() || token.equals(expanded)) {
            return "";
        }
        return capitalize(token) + "→" + capitalize(expanded);
    }

    private static String expandSuffix(String token) {
        return SUFFIXES.getOrDefault(token, token);
    }

    private static String normalizeBase(String value) {
        if (value == null) {
            return "";
        }
        String nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC);
        return nfkc.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    /** Replace punctuation with space; keep letters, digits, and spaces. */
    private static String stripUnsafePunctuation(String value) {
        return value.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String capitalize(String token) {
        if (token.isEmpty()) {
            return token;
        }
        return token.substring(0, 1).toUpperCase(Locale.ROOT) + token.substring(1);
    }
}
