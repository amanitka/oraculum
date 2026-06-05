package com.oraculum.harvester.api;

import com.oraculum.harvester.api.dto.HarvesterRequest;

public interface HarvesterRequestApi {
    void publish(HarvesterRequest request);
}
