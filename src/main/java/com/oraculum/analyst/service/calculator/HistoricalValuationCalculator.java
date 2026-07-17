package com.oraculum.analyst.service.calculator;

import com.oraculum.analyst.dto.HistoricalValuationSummary;
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

    public List<HistoricalValuationSummary> calculate(List<SharePriceSignalDto> dailySignals, List<SharePriceSignalDto> monthlySignals) {
        if (dailySignals == null || dailySignals.isEmpty() || monthlySignals == null || monthlySignals.isEmpty()) {
            return List.of();
        }

        SharePriceSignalDto latest = dailySignals.getFirst();
        LocalDate maxDate = latest.tradeDate();
        if (maxDate == null) return List.of();

        List<SharePriceSignalDto> list5y = filterSignals5Y(monthlySignals, maxDate);
        List<HistoricalValuationSummary> summaries = new ArrayList<>();

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

    private void addMetric(List<HistoricalValuationSummary> list, String name, Float current,
                           List<SharePriceSignalDto> history10y, List<SharePriceSignalDto> history5y,
                           Function<SharePriceSignalDto, Float> mapper) {
        if (current == null) return;
        List<Float> valid10y = mapAndFilterNulls(history10y, mapper);
        List<Float> valid5y = mapAndFilterNulls(history5y, mapper);
        if (valid10y.isEmpty()) return;

        list.add(computeSummary(name, current, valid10y, valid5y));
    }

    private List<Float> mapAndFilterNulls(List<SharePriceSignalDto> signals, Function<SharePriceSignalDto, Float> mapper) {
        return signals.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private HistoricalValuationSummary computeSummary(String name, float current, List<Float> valid10y, List<Float> valid5y) {
        double sum10y = 0;
        float min10y = Float.MAX_VALUE;
        float max10y = -Float.MAX_VALUE;
        int countBelow = 0;
        for (float val : valid10y) {
            sum10y += val;
            min10y = Math.min(min10y, val);
            max10y = Math.max(max10y, val);
            if (val <= current) countBelow++;
        }
        float avg10y = (float) (sum10y / valid10y.size());
        int percentile10y = (int) Math.round((double) countBelow / valid10y.size() * 100);
        float avg5y = computeAverage(valid5y, avg10y);

        return new HistoricalValuationSummary(name, current, avg5y, avg10y, percentile10y, min10y, max10y);
    }

    private float computeAverage(List<Float> values, float fallback) {
        return values.isEmpty() ? fallback : (float) (values.stream().mapToDouble(Float::doubleValue).average().orElse(fallback));
    }
}
