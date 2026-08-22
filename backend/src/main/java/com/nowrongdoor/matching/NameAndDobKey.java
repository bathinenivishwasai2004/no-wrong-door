package com.nowrongdoor.matching;

/**
 * Normalized name plus canonical date of birth. Built only when DOB is present.
 */
public record NameAndDobKey(NameKey name, String dob) {}
