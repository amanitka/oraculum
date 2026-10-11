package com.oraculum.ui.components;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.TooltipBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.chart.builder.EventsBuilder;
import com.github.appreciated.apexcharts.config.plotoptions.builder.TreemapBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.CompanyOverviewDto;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class InstitutionalFlowTreemapComponent extends Div {

    public InstitutionalFlowTreemapComponent() {
        setSizeFull();
    }

    public void updateData(List<CompanyOverviewDto> companies,
                           Map<Integer, CompanyInstitutionalFlowDto> instFlowMap,
                           String selectedMetric) {
        removeAll();
        if (companies == null || companies.isEmpty() || instFlowMap == null || instFlowMap.isEmpty()) {
            add(new Paragraph("No institutional 13F holding data available for this quarter."));
            return;
        }

        // Group companies that have 13F institutional holding value
        Map<String, List<CompanyOverviewDto>> groupedBySector = companies.stream()
                .filter(c -> c.sectorName() != null && instFlowMap.containsKey(c.companyId()))
                .filter(c -> {
                    CompanyInstitutionalFlowDto flow = instFlowMap.get(c.companyId());
                    return flow.totalInstValueUsd() != null && flow.totalInstValueUsd() > 0;
                })
                .collect(Collectors.groupingBy(CompanyOverviewDto::sectorName));

        if (groupedBySector.isEmpty()) {
            add(new Paragraph("No institutional 13F holding data available for this quarter."));
            return;
        }

        List<Series<InstTreemapData>> seriesList = new ArrayList<>();
        for (Map.Entry<String, List<CompanyOverviewDto>> entry : groupedBySector.entrySet()) {
            List<CompanyOverviewDto> topCompanies = entry.getValue().stream()
                    .sorted((a, b) -> {
                        Long va = instFlowMap.get(a.companyId()).totalInstValueUsd();
                        Long vb = instFlowMap.get(b.companyId()).totalInstValueUsd();
                        return Long.compare(vb != null ? vb : 0L, va != null ? va : 0L);
                    })
                    .limit(20)
                    .toList();

            List<InstTreemapData> dataPoints = topCompanies.stream()
                    .map(c -> toCustomData(c, instFlowMap.get(c.companyId()), selectedMetric))
                    .toList();

            seriesList.add(new Series<>(entry.getKey(), dataPoints.toArray(new InstTreemapData[0])));
        }

        ApexCharts treemap = buildApexCharts(seriesList);
        add(Objects.requireNonNullElseGet(treemap, () -> new Paragraph("No institutional 13F holding data available.")));
    }

    private InstTreemapData toCustomData(CompanyOverviewDto c,
                                         CompanyInstitutionalFlowDto flow,
                                         String selectedMetric) {
        String color = getColorForMetric(flow, selectedMetric);
        String name = c.companyName() != null ? c.companyName() : c.ticker();
        Float instValBillions = flow.totalInstValueUsd() != null ? flow.totalInstValueUsd() / 1_000_000_000f : 0f;

        String buyers = flow.tier1BuyerNames() != null ? ViewHelper.formatFundNames(flow.tier1BuyerNames()) : null;
        String sellers = flow.tier1SellerNames() != null ? ViewHelper.formatFundNames(flow.tier1SellerNames()) : null;
        String topHolders = flow.topHoldersNames() != null ? ViewHelper.formatFundNames(flow.topHoldersNames()) : null;

        return new InstTreemapData(
                c.ticker(),
                instValBillions,
                color,
                name,
                c.companyId(),
                flow.reportPeriod() != null ? flow.reportPeriod().toString() : "N/A",
                flow.totalInstValueUsd(),
                flow.totalInstShares(),
                flow.netValueChangeUsd(),
                flow.netSharesChange(),
                flow.sharesChangePct(),
                flow.totalHoldersCount(),
                flow.putCallRatio(),
                flow.top5HoldersPct(),
                flow.tier1ValueUsd(),
                flow.tier1NetSharesChange(),
                flow.tier1NetValueChangeUsd(),
                flow.tier1HoldersCount(),
                flow.tier1BuyersCount(),
                flow.tier1SellersCount(),
                buyers,
                sellers,
                topHolders
        );
    }

    private String getColorForMetric(CompanyInstitutionalFlowDto flow, String metric) {
        if (flow == null) return "#334155";
        if ("Smart Money (Tier 1 Flow)".equals(metric)) {
            Long s = flow.tier1NetSharesChange();
            if (s == null) return "#334155";
            if (s > 1_000_000L) return "#16a34a";
            if (s > 0) return "#15803d";
            if (s < -1_000_000L) return "#dc2626";
            if (s < 0) return "#b91c1c";
            return "#334155";
        }
        if ("Put / Call Sentiment".equals(metric)) {
            Float r = flow.putCallRatio();
            if (r == null) return "#334155";
            if (r < 0.70f) return "#16a34a";
            if (r < 0.95f) return "#15803d";
            if (r <= 1.15f) return "#334155";
            if (r <= 1.40f) return "#b91c1c";
            return "#dc2626";
        }
        // Default: Institutional Net Flow %
        Float pct = flow.sharesChangePct();
        if (pct == null) return "#334155";
        if (pct >= 5.0f) return "#16a34a";
        if (pct >= 1.5f) return "#15803d";
        if (pct > 0.0f) return "#166534";
        if (pct <= -5.0f) return "#dc2626";
        if (pct <= -1.5f) return "#b91c1c";
        if (pct < 0.0f) return "#991b1b";
        return "#334155";
    }

    private ApexCharts buildApexCharts(List<Series<InstTreemapData>> seriesList) {
        String tooltipCustomJs = """
                function({ series, seriesIndex, dataPointIndex, w }) {
                    var item = w.config.series[seriesIndex].data[dataPointIndex];
                    if (!item) return '';
                
                    var name = item.companyName || item.x;
                    var ticker = item.x;
                    var instVal = item.instValUsd ? '$' + (item.instValUsd / 1e9).toFixed(2) + 'B' : 'N/A';
                    var instShares = item.instShares ? (item.instShares / 1e6).toFixed(2) + 'M sh' : 'N/A';
                
                    var formatPct = function(val) {
                        if (val === null || val === undefined) return '<span style="color:#aaa;">N/A</span>';
                        var color = val >= 0 ? '#4caf50' : '#f44336';
                        var sign = val >= 0 ? '+' : '';
                        return '<span style="color:' + color + '; font-weight:600;">' + sign + val.toFixed(2) + '%</span>';
                    };
                
                    var formatShares = function(val) {
                        if (val === null || val === undefined) return '<span style="color:#aaa;">N/A</span>';
                        var sign = val >= 0 ? '+' : '';
                        var color = val >= 0 ? '#4caf50' : '#f44336';
                        var abs = Math.abs(val);
                        var formatted = abs >= 1e9 ? (abs / 1e9).toFixed(2) + 'B' : (abs >= 1e6 ? (abs / 1e6).toFixed(2) + 'M' : abs.toLocaleString());
                        return '<span style="color:' + color + '; font-weight:600;">' + sign + formatted + '</span>';
                    };
                
                    var formatValUsd = function(val) {
                        if (val === null || val === undefined) return '<span style="color:#aaa;">N/A</span>';
                        var sign = val >= 0 ? '+$' : '-$';
                        var color = val >= 0 ? '#4caf50' : '#f44336';
                        var abs = Math.abs(val);
                        var formatted = abs >= 1e9 ? (abs / 1e9).toFixed(2) + 'B' : (abs >= 1e6 ? (abs / 1e6).toFixed(2) + 'M' : abs.toLocaleString());
                        return '<span style="color:' + color + '; font-weight:600;">' + sign + formatted + '</span>';
                    };
                
                    var html = '<div style="padding: 12px 16px; background: rgba(18, 24, 38, 0.96); border: 1px solid rgba(56, 189, 248, 0.3); border-radius: 8px; color: #fff; font-family: var(--lumo-font-family, sans-serif); box-shadow: 0 4px 20px rgba(0,0,0,0.5); min-width: 270px; max-width: 350px;">' +
                           '<div style="font-size: 14px; font-weight: 700; color: #f0f4f8;">' + name + ' <span style="color:#38bdf8; font-size:12px; font-weight:600;">(' + ticker + ')</span></div>' +
                           '<div style="font-size: 11px; margin-bottom: 6px; color: #94a3b8;">SEC 13F Period: <strong style="color:#fff;">' + (item.reportPeriod || 'N/A') + '</strong></div>' +
                           '<hr style="border: 0; border-top: 1px solid rgba(255,255,255,0.1); margin: 6px 0;"/>' +
                           '<div style="font-size: 12px; display: grid; grid-template-columns: 110px 1fr; gap: 4px;">' +
                           '<span style="color:#94a3b8;">Total Portfolio:</span><span style="color:#fff; font-weight:600;">' + instVal + ' (' + instShares + ')</span>' +
                           '<span style="color:#94a3b8;">QoQ Total Flow:</span>' + formatPct(item.sharesChangePct) +
                           '<span style="color:#94a3b8;">Total Net Shares:</span>' + formatShares(item.netSharesChange) +
                           '<span style="color:#94a3b8;">Holders Count:</span><span style="color:#fff;">' + (item.totalHoldersCount ? item.totalHoldersCount.toLocaleString() : 'N/A') + '</span>' +
                           '<span style="color:#94a3b8;">Put / Call Ratio:</span><span style="color:#fff;">' + (item.putCallRatio ? item.putCallRatio.toFixed(2) : 'N/A') + '</span>' +
                           '</div>';
                
                    // Smart Money Tier 1 Section with exact numbers
                    html += '<hr style="border: 0; border-top: 1px solid rgba(255,255,255,0.1); margin: 6px 0;"/>' +
                            '<div style="font-size: 11px; font-weight: 700; color: #f59e0b; margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.5px;">Smart Money (Tier 1 Superinvestors)</div>' +
                            '<div style="font-size: 12px; display: grid; grid-template-columns: 110px 1fr; gap: 4px;">' +
                            '<span style="color:#94a3b8;">Tier 1 Value Δ:</span>' + formatValUsd(item.tier1NetValChange) +
                            '<span style="color:#94a3b8;">Tier 1 Shares Δ:</span>' + formatShares(item.tier1NetShares) +
                            '<span style="color:#94a3b8;">Tier 1 Activity:</span><span style="color:#fff;">' + (item.tier1BuyersCount || 0) + ' buy / ' + (item.tier1SellersCount || 0) + ' sell</span>' +
                            '</div>';
                
                    if (item.tier1Buyers) {
                        html += '<div style="font-size: 11px; margin-top: 4px; color: #4caf50;"><strong>Tier 1 BUY:</strong> ' + item.tier1Buyers + '</div>';
                    }
                    if (item.tier1Sellers) {
                        html += '<div style="font-size: 11px; margin-top: 2px; color: #f44336;"><strong>Tier 1 SELL:</strong> ' + item.tier1Sellers + '</div>';
                    }
                
                    html += '</div>';
                    return html;
                }
                """;

        String clickEventJs = """
                function(event, chartContext, config) {
                    if (config && config.w && config.seriesIndex !== undefined && config.dataPointIndex !== undefined) {
                        var item = config.w.config.series[config.seriesIndex].data[config.dataPointIndex];
                        if (item && item.companyId && window.oraculumMarketMapClick) {
                            window.oraculumMarketMapClick(item.companyId);
                        }
                    }
                }
                """;

        return ApexChartsBuilder.get()
                .withChart(ChartBuilder.get()
                        .withType(Type.TREEMAP)
                        .withHeight("800px")
                        .withEvents(EventsBuilder.get()
                                .withDataPointSelection(clickEventJs)
                                .build())
                        .build())
                .withSeries(seriesList.toArray(new Series[0]))
                .withPlotOptions(PlotOptionsBuilder.get()
                        .withTreemap(TreemapBuilder.get()
                                .withEnableShades(true)
                                .withShadeIntensity(0.5)
                                .withDistributed(false)
                                .build())
                        .build())
                .withDataLabels(DataLabelsBuilder.get()
                        .withEnabled(true)
                        .build())
                .withTooltip(TooltipBuilder.get()
                        .withCustom(tooltipCustomJs)
                        .build())
                .build();
    }

    public static class InstTreemapData {
        public String x;
        public Float y; // Tile size in Billions USD of institutional holding value
        public String fillColor;
        public String companyName;
        public int companyId;
        public String reportPeriod;
        public Long instValUsd;
        public Long instShares;
        public Long netValChangeUsd;
        public Long netSharesChange;
        public Float sharesChangePct;
        public Integer totalHoldersCount;
        public Float putCallRatio;
        public Float top5HoldersPct;

        // Smart Money Tier 1 specific metrics
        public Long tier1ValUsd;
        public Long tier1NetShares;
        public Long tier1NetValChange;
        public Integer tier1HoldersCount;
        public Integer tier1BuyersCount;
        public Integer tier1SellersCount;
        public String tier1Buyers;
        public String tier1Sellers;
        public String topHolders;

        public InstTreemapData(String x, Float y, String fillColor, String companyName, int companyId,
                               String reportPeriod, Long instValUsd, Long instShares, Long netValChangeUsd,
                               Long netSharesChange, Float sharesChangePct, Integer totalHoldersCount,
                               Float putCallRatio, Float top5HoldersPct,
                               Long tier1ValUsd, Long tier1NetShares, Long tier1NetValChange,
                               Integer tier1HoldersCount, Integer tier1BuyersCount, Integer tier1SellersCount,
                               String tier1Buyers, String tier1Sellers, String topHolders) {
            this.x = x;
            this.y = y;
            this.fillColor = fillColor;
            this.companyName = companyName;
            this.companyId = companyId;
            this.reportPeriod = reportPeriod;
            this.instValUsd = instValUsd;
            this.instShares = instShares;
            this.netValChangeUsd = netValChangeUsd;
            this.netSharesChange = netSharesChange;
            this.sharesChangePct = sharesChangePct;
            this.totalHoldersCount = totalHoldersCount;
            this.putCallRatio = putCallRatio;
            this.top5HoldersPct = top5HoldersPct;
            this.tier1ValUsd = tier1ValUsd;
            this.tier1NetShares = tier1NetShares;
            this.tier1NetValChange = tier1NetValChange;
            this.tier1HoldersCount = tier1HoldersCount;
            this.tier1BuyersCount = tier1BuyersCount;
            this.tier1SellersCount = tier1SellersCount;
            this.tier1Buyers = tier1Buyers;
            this.tier1Sellers = tier1Sellers;
            this.topHolders = topHolders;
        }
    }
}
