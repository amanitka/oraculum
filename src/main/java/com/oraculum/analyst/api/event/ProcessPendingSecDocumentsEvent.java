package com.oraculum.analyst.api.event;

public record ProcessPendingSecDocumentsEvent(int limit, int maxPriority) {
}
