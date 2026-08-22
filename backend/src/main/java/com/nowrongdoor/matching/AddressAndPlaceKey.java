package com.nowrongdoor.matching;

/**
 * Normalized street line plus city/town. Never used as a standalone match key.
 */
public record AddressAndPlaceKey(String line, String place) {}
