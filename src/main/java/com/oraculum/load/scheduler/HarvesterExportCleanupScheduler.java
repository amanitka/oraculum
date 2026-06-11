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
@ConditionalOnProperty(name = "oraculum.harvester.export-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class HarvesterExportCleanupScheduler {

    private final OraculumProperties properties;

    @Scheduled(cron = "${oraculum.harvester.export-cleanup.cron}")
    public void cleanupExportFolder() {
        log.info("Starting scheduled harvester export cleanup...");
        try {
            String exportPathStr = properties.harvester().exportPath();
            Integer retentionDays = properties.harvester().exportCleanup().retentionDays();

            Path exportPath = Path.of(exportPathStr);
            Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

            FileCleanupUtil.deleteFilesOlderThan(exportPath, cutoff);
            log.info("Scheduled harvester export cleanup completed.");
        } catch (Exception e) {
            log.error("Failed to run scheduled harvester export cleanup", e);
        }
    }
}
