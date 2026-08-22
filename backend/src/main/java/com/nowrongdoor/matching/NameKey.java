package com.nowrongdoor.matching;

/**
 * Normalized given + surname. Used as the only candidate-generation key for names.
 */
public record NameKey(String given, String surname) {}
