package com.nowrongdoor.api;

import java.util.List;

public record SearchResponse(List<ResidentResponse> results, int totalResults) {}
