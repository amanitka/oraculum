package com.oraculum.load.scheduler;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.common.util.FileCleanupUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oraculum.harvester.data-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class HarvesterDataCleanupScheduler {

    private final OraculumProperties properties;

    @Scheduled(cron = "${oraculum.harvester.data-cleanup.cron}")
    public void cleanupHarvesterDataFolder() {
        log.info("Starting scheduled harvester data cleanup...");
        try {
            String exportPathStr = properties.harvester().dataPath();
            Integer retentionDays = properties.harvester().dataCleanup().retentionDays();

            Path exportPath = Path.of(exportPathStr);
            Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

            FileCleanupUtil.deleteFilesOlderThan(exportPath, cutoff);
            log.info("Scheduled harvester data cleanup completed.");
        } catch (Exception e) {
            log.error("Failed to run scheduled harvester data cleanup", e);
        }
    }
}
