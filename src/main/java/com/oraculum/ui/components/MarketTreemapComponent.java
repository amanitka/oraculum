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

public class MarketTreemapComponent extends Div {

    public MarketTreemapComponent() {
        setSizeFull();
    }

    public void updateData(List<CompanyOverviewDto> companies,
                           Map<Integer, CompanyInstitutionalFlowDto> instFlowMap,
                           String selectedMetric) {
        removeAll();
        if (companies == null || companies.isEmpty()) {
            add(new Paragraph("Not enough data to display the market map."));
            return;
        }

        Map<String, List<CompanyOverviewDto>> groupedBySector = companies.stream()
                .filter(c -> c.sectorName() != null && c.marketCapitalization() != null && c.marketCapitalization() > 0)
                .collect(Collectors.groupingBy(CompanyOverviewDto::sectorName));

        List<Series<TreemapCustomData>> seriesList = new ArrayList<>();
        for (Map.Entry<String, List<CompanyOverviewDto>> entry : groupedBySector.entrySet()) {
            List<CompanyOverviewDto> topCompanies = entry.getValue().stream()
                    .sorted((a, b) -> Float.compare(b.marketCapitalization(), a.marketCapitalization()))
                    .limit(20)
                    .toList();

            List<TreemapCustomData> dataPoints = topCompanies.stream()
                    .map(c -> toCustomData(c, instFlowMap != null ? instFlowMap.get(c.companyId()) : null, selectedMetric))
                    .toList();

            seriesList.add(new Series<>(entry.getKey(), dataPoints.toArray(new TreemapCustomData[0])));
        }

        ApexCharts treemap = buildApexCharts(seriesList);
        add(Objects.requireNonNullElseGet(treemap, () -> new Paragraph("Not enough data to display the market map.")));
    }

    private TreemapCustomData toCustomData(CompanyOverviewDto c,
                                          CompanyInstitutionalFlowDto flow,
                                          String selectedMetric) {
        String color = getColorForMetric(c, flow, selectedMetric);
        String name = c.companyName() != null ? c.companyName() : c.ticker();

        String buyers = flow != null ? ViewHelper.formatFundNames(flow.tier1BuyerNames()) : null;
        String sellers = flow != null ? ViewHelper.formatFundNames(flow.tier1SellerNames()) : null;
        String topHolders = flow != null ? ViewHelper.formatFundNames(flow.topHoldersNames()) : null;

        return new TreemapCustomData(
                c.ticker(),
                c.marketCapitalization(),
                color,
                name,
                c.companyId(),
                c.priceChange1d(),
                c.priceChange1w(),
                c.priceChange1m(),
                c.priceChange6m(),
                c.priceChange1y(),
                c.pctFrom50dMa(),
                c.pctFrom200dMa(),
                c.volumeVelocity(),
                c.tradeDate() != null ? c.tradeDate().toString() : "N/A",
                ViewHelper.getCurrencySymbol(c.currency(), c.market()),
                flow != null ? flow.sharesChangePct() : null,
                flow != null ? flow.netSharesChange() : null,
                flow != null ? flow.totalHoldersCount() : null,
                flow != null ? flow.putCallRatio() : null,
                flow != null ? flow.top5HoldersPct() : null,
                buyers,
                sellers,
                topHolders
        );
    }

    private String getColorForMetric(CompanyOverviewDto c,
                                     CompanyInstitutionalFlowDto flow,
                                     String metric) {
        if ("Price: 1-Week Change".equals(metric)) {
            return getColorForPctChange(c.priceChange1w());
        }
        if ("Price: 1-Month Change".equals(metric)) {
            return getColorForPctChange(c.priceChange1m());
        }
        if ("13F: Institutional Net Flow %".equals(metric)) {
            return getColorForPctChange(flow != null ? flow.sharesChangePct() : null);
        }
        if ("13F: Put / Call Sentiment".equals(metric)) {
            return getColorForPutCall(flow != null ? flow.putCallRatio() : null);
        }
        if ("13F: Smart Money (Tier 1 Flow)".equals(metric)) {
            return getColorForTier1(flow != null ? flow.tier1NetSharesChange() : null);
        }
        return getColorForPctChange(c.priceChange1d());
    }

    private String getColorForPctChange(Float pctChange) {
        if (pctChange == null) return "#334155";
        if (pctChange >= 3.0f) return "#16a34a";
        if (pctChange >= 1.0f) return "#15803d";
        if (pctChange > 0.0f) return "#166534";
        if (pctChange <= -3.0f) return "#dc2626";
        if (pctChange <= -1.0f) return "#b91c1c";
        if (pctChange < 0.0f) return "#991b1b";
        return "#334155";
    }

    private String getColorForPutCall(Float r) {
        if (r == null) return "#334155";
        if (r < 0.70f) return "#16a34a";
        if (r < 0.95f) return "#15803d";
        if (r <= 1.15f) return "#334155";
        if (r <= 1.40f) return "#b91c1c";
        return "#dc2626";
    }

    private String getColorForTier1(Long shares) {
        if (shares == null) return "#334155";
        if (shares > 1_000_000L) return "#16a34a";
        if (shares > 0) return "#15803d";
        if (shares < -1_000_000L) return "#dc2626";
        if (shares < 0) return "#b91c1c";
        return "#334155";
    }

    private ApexCharts buildApexCharts(List<Series<TreemapCustomData>> seriesList) {
        String tooltipCustomJs = """
                function({ series, seriesIndex, dataPointIndex, w }) {
                    var item = w.config.series[seriesIndex].data[dataPointIndex];
                    if (!item) return '';
                
                    var name = item.companyName || item.x;
                    var ticker = item.x;
                    var symbol = item.currencySymbol || '$';
                    var cap = item.y ? symbol + (item.y / 1000000000).toFixed(2) + 'B' : 'N/A';
                
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
                
                    var html = '<div style="padding: 12px 16px; background: rgba(18, 24, 38, 0.95); border: 1px solid rgba(255,255,255,0.12); border-radius: 8px; color: #fff; font-family: var(--lumo-font-family, sans-serif); box-shadow: 0 4px 16px rgba(0,0,0,0.4); min-width: 240px; max-width: 320px;">' +
                           '<div style="font-size: 14px; font-weight: 700; margin-bottom: 4px; color: #f0f4f8;">' + name + ' <span style="color:#8a99ad; font-size:12px; font-weight:400;">(' + ticker + ')</span></div>' +
                           '<div style="font-size: 12px; margin-bottom: 2px; color: #cbd5e1;">Market Cap: <strong style="color:#fff;">' + cap + '</strong></div>' +
                           '<div style="font-size: 12px; margin-bottom: 6px; color: #cbd5e1;">Last Trade: <strong style="color:#fff;">' + (item.tradeDate || 'N/A') + '</strong></div>' +
                           '<hr style="border: 0; border-top: 1px solid rgba(255,255,255,0.1); margin: 6px 0;"/>' +
                           '<div style="font-size: 12px; display: grid; grid-template-columns: 85px 1fr; gap: 4px;">' +
                           '<span style="color:#94a3b8;">Price 1D:</span>' + formatPct(item.priceChange1d) +
                           '<span style="color:#94a3b8;">Price 1W:</span>' + formatPct(item.priceChange1w) +
                           '<span style="color:#94a3b8;">Price 1M:</span>' + formatPct(item.priceChange1m) +
                           '</div>';
                
                    if (item.instFlowPct !== null && item.instFlowPct !== undefined) {
                        html += '<hr style="border: 0; border-top: 1px solid rgba(255,255,255,0.1); margin: 6px 0;"/>' +
                                '<div style="font-size: 11px; font-weight: 700; color: #38bdf8; margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.5px;">Institutional 13F Flow</div>' +
                                '<div style="font-size: 12px; display: grid; grid-template-columns: 85px 1fr; gap: 4px;">' +
                                '<span style="color:#94a3b8;">Net Flow %:</span>' + formatPct(item.instFlowPct) +
                                '<span style="color:#94a3b8;">Net Shares:</span>' + formatShares(item.netSharesChange) +
                                '<span style="color:#94a3b8;">Holders:</span><span style="color:#fff;">' + (item.totalHoldersCount ? item.totalHoldersCount.toLocaleString() : 'N/A') + '</span>' +
                                '<span style="color:#94a3b8;">P/C Ratio:</span><span style="color:#fff;">' + (item.putCallRatio ? item.putCallRatio.toFixed(2) : 'N/A') + '</span>' +
                                '<span style="color:#94a3b8;">Top 5 Conc.:</span><span style="color:#fff;">' + (item.top5HoldersPct ? item.top5HoldersPct.toFixed(1) + '%' : 'N/A') + '</span>' +
                                '</div>';
                
                        if (item.tier1Buyers) {
                            html += '<div style="font-size: 11px; margin-top: 4px; color: #4caf50;"><strong>Smart Money BUY:</strong> ' + item.tier1Buyers + '</div>';
                        }
                        if (item.tier1Sellers) {
                            html += '<div style="font-size: 11px; margin-top: 2px; color: #f44336;"><strong>Smart Money SELL:</strong> ' + item.tier1Sellers + '</div>';
                        }
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

    public static class TreemapCustomData {
        public String x;
        public Float y;
        public String fillColor;
        public String companyName;
        public int companyId;
        public Float priceChange1d;
        public Float priceChange1w;
        public Float priceChange1m;
        public Float priceChange6m;
        public Float priceChange1y;
        public Float pctFrom50dMa;
        public Float pctFrom200dMa;
        public Float volumeVelocity;
        public String tradeDate;
        public String currencySymbol;
        public Float instFlowPct;
        public Long netSharesChange;
        public Integer totalHoldersCount;
        public Float putCallRatio;
        public Float top5HoldersPct;
        public String tier1Buyers;
        public String tier1Sellers;
        public String topHolders;

        public TreemapCustomData(String x, Float y, String fillColor, String companyName, int companyId,
                                 Float priceChange1d, Float priceChange1w, Float priceChange1m,
                                 Float priceChange6m, Float priceChange1y,
                                 Float pctFrom50dMa, Float pctFrom200dMa, Float volumeVelocity, String tradeDate, String currencySymbol,
                                 Float instFlowPct, Long netSharesChange, Integer totalHoldersCount, Float putCallRatio,
                                 Float top5HoldersPct, String tier1Buyers, String tier1Sellers, String topHolders) {
            this.x = x;
            this.y = y;
            this.fillColor = fillColor;
            this.companyName = companyName;
            this.companyId = companyId;
            this.priceChange1d = priceChange1d;
            this.priceChange1w = priceChange1w;
            this.priceChange1m = priceChange1m;
            this.priceChange6m = priceChange6m;
            this.priceChange1y = priceChange1y;
            this.pctFrom50dMa = pctFrom50dMa;
            this.pctFrom200dMa = pctFrom200dMa;
            this.volumeVelocity = volumeVelocity;
            this.tradeDate = tradeDate;
            this.currencySymbol = currencySymbol;
            this.instFlowPct = instFlowPct;
            this.netSharesChange = netSharesChange;
            this.totalHoldersCount = totalHoldersCount;
            this.putCallRatio = putCallRatio;
            this.top5HoldersPct = top5HoldersPct;
            this.tier1Buyers = tier1Buyers;
            this.tier1Sellers = tier1Sellers;
            this.topHolders = topHolders;
        }
    }
}
