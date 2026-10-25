package com.oraculum.harvester.scheduler;

import com.oraculum.harvester.service.FredService;
import com.oraculum.harvester.service.HarvesterBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshSchedulerTest {

    @Mock
    private HarvesterBatchService refreshService;

    @Mock
    private FredService fredService;

    private RefreshScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RefreshScheduler(refreshService, fredService);
    }

    @Test
    void refreshCompanyCusips_delegatesToRefreshService() {
        scheduler.refreshCompanyCusips();
        verify(refreshService).refreshCompanyCusips();
    }
}
