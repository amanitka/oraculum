package com.oraculum.company.service.calculator;

import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class HistoricalValuationCalculator {

    // Metrics where a negative reading represents a different regime (losses)
    // rather than a genuinely "cheap" valuation, and shouldn't be compared
    // on the same axis as positive multiples for min/max/percentile/average.
    private static final List<String> EARNINGS_MULTIPLE_METRICS = List.of("P/E", "EV/EBITDA");

    public List<HistoricalValuationSummaryDto> calculate(List<SharePriceSignalDto> dailySignals, List<SharePriceSignalDto> monthlySignals) {
        if (dailySignals == null || dailySignals.isEmpty() || monthlySignals == null || monthlySignals.isEmpty()) {
            return List.of();
        }

        SharePriceSignalDto latest = dailySignals.getFirst();
        LocalDate maxDate = latest.tradeDate();
        if (maxDate == null) return List.of();

        List<SharePriceSignalDto> list5y = filterSignals5Y(monthlySignals, maxDate);
        List<HistoricalValuationSummaryDto> summaries = new ArrayList<>();

        addMetric(summaries, "P/E", latest.peRatio(), monthlySignals, list5y, SharePriceSignalDto::peRatio);
        addMetric(summaries, "EV/EBITDA", latest.enterpriseValueToEbitda(), monthlySignals, list5y, SharePriceSignalDto::enterpriseValueToEbitda);
        addMetric(summaries, "FCF Yield", latest.fcfYield(), monthlySignals, list5y, SharePriceSignalDto::fcfYield);
        addMetric(summaries, "P/S", latest.priceToSales(), monthlySignals, list5y, SharePriceSignalDto::priceToSales);

        return summaries;
    }

    private List<SharePriceSignalDto> filterSignals5Y(List<SharePriceSignalDto> signals, LocalDate maxDate) {
        LocalDate cutoff5Y = maxDate.minusYears(5);
        return signals.stream()
                .filter(s -> s.tradeDate() != null && s.tradeDate().isAfter(cutoff5Y))
                .collect(Collectors.toList());
    }

    private void addMetric(List<HistoricalValuationSummaryDto> list, String name, Float current,
                           List<SharePriceSignalDto> history10y, List<SharePriceSignalDto> history5y,
                           Function<SharePriceSignalDto, Float> mapper) {
        if (current == null) return;

        List<Double> raw10y = mapAndFilterNulls(history10y, mapper);
        List<Double> raw5y = mapAndFilterNulls(history5y, mapper);
        if (raw10y.isEmpty()) return;

        // Apply the same filtering to every statistic derived from this
        // metric — min, max, percentile, and average all describe the same
        // population now, instead of min/max/percentile using the raw
        // (loss-period-included) list while average silently used a
        // filtered one. If filtering empties the set (e.g. every period was
        // loss-making), fall back to the unfiltered set rather than
        // dropping the metric.
        List<Double> comparable10y = filterComparablePopulation(name, raw10y);
        List<Double> comparable5y = filterComparablePopulation(name, raw5y);

        list.add(computeSummary(name, current, comparable10y, comparable5y));
    }

    private List<Double> mapAndFilterNulls(List<SharePriceSignalDto> signals, Function<SharePriceSignalDto, Float> mapper) {
        return signals.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .map(Float::doubleValue)
                .collect(Collectors.toList());
    }

    private List<Double> filterComparablePopulation(String name, List<Double> values) {
        if (!EARNINGS_MULTIPLE_METRICS.contains(name)) {
            return values;
        }
        List<Double> positive = values.stream().filter(v -> v > 0).collect(Collectors.toList());
        return positive.isEmpty() ? values : positive;
    }

    private HistoricalValuationSummaryDto computeSummary(String name, float current, List<Double> comparable10y, List<Double> comparable5y) {
        double min10y = Double.MAX_VALUE;
        double max10y = -Double.MAX_VALUE;
        int countBelow = 0;
        for (double val : comparable10y) {
            min10y = Math.min(min10y, val);
            max10y = Math.max(max10y, val);
            if (val <= current) countBelow++;
        }
        int percentile10y = (int) Math.round((double) countBelow / comparable10y.size() * 100);

        double avg10y = computeAverage(comparable10y, current);
        double avg5y = computeAverage(comparable5y, avg10y);

        return new HistoricalValuationSummaryDto(name, current, (float) avg5y, (float) avg10y, percentile10y, (float) min10y, (float) max10y);
    }

    private double computeAverage(List<Double> values, double fallback) {
        return values.isEmpty() ? fallback : values.stream().mapToDouble(Double::doubleValue).average().orElse(fallback);
    }
}
