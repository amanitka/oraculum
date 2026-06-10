package com.oraculum.database.listener;

import com.oraculum.database.api.event.RefreshMaterializedViewsEvent;
import com.oraculum.database.service.DatabaseMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseEventListener {

    private final DatabaseMaintenanceService databaseMaintenanceService;

    @Async
    @EventListener
    public void onRefreshMaterializedViewsEvent(RefreshMaterializedViewsEvent ignoredEvent) {
        databaseMaintenanceService.refreshMaterializedViews();
    }
}
