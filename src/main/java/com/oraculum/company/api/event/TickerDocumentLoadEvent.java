package com.oraculum.company.api.event;

import com.oraculum.load.api.dto.DataFileStatus;

import java.util.List;

public record TickerDocumentLoadEvent(
        List<DataFileStatus> fileStatuses
) {
}
