package com.oraculum.analyst.service.valuation;

import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Component
public class EpsReconciliationCalculator {

    public record EpsReconciliationResult(boolean divergence, String status, String note) {}

    public EpsReconciliationResult reconcile(
            EarningsEstimateDto fy,
            List<EarningsEstimateDto> futureQuarters,
            List<CompanyFinancialRatiosDto> quarterlyRatios
    ) {
        if (fy == null || fy.epsEstimateAverage() == null || fy.date() == null) {
            return new EpsReconciliationResult(false, null, null);
        }
        double fyAvg = fy.epsEstimateAverage().doubleValue();
        if (fyAvg <= 0.001) {
            return new EpsReconciliationResult(false, null, null);
        }
        LocalDate fyStart = fy.date().minusYears(1);
        List<EarningsEstimateDto> fyFutureQuarters = filterFutureQuartersForFy(futureQuarters, fyStart, fy.date());
        List<CompanyFinancialRatiosDto> fyActualQuarters = filterActualQuartersForFy(quarterlyRatios, fyStart, fy.date(), fyFutureQuarters);

        return evaluateFourQuarterReconciliation(fy.date(), fyAvg, fyActualQuarters, fyFutureQuarters);
    }

    private List<EarningsEstimateDto> filterFutureQuartersForFy(List<EarningsEstimateDto> quarters, LocalDate fyStart, LocalDate fyEnd) {
        if (quarters == null) return List.of();
        return quarters.stream()
                .filter(q -> q.date() != null && q.date().isAfter(fyStart) && !q.date().isAfter(fyEnd))
                .filter(q -> q.epsEstimateAverage() != null)
                .toList();
    }

    private List<CompanyFinancialRatiosDto> filterActualQuartersForFy(
            List<CompanyFinancialRatiosDto> quarterlyRatios,
            LocalDate fyStart,
            LocalDate fyEnd,
            List<EarningsEstimateDto> futureQuarters
    ) {
        if (quarterlyRatios == null || quarterlyRatios.isEmpty()) return List.of();
        return quarterlyRatios.stream()
                .filter(r -> r.reportDate() != null && r.reportDate().isAfter(fyStart) && !r.reportDate().isAfter(fyEnd))
                .filter(r -> r.earningsPerShare() != null)
                .filter(r -> !overlapsFutureQuarter(r.reportDate(), futureQuarters))
                .toList();
    }

    private boolean overlapsFutureQuarter(LocalDate reportDate, List<EarningsEstimateDto> futureQuarters) {
        return futureQuarters.stream()
                .anyMatch(q -> Math.abs(ChronoUnit.DAYS.between(reportDate, q.date())) <= 20);
    }

    private EpsReconciliationResult evaluateFourQuarterReconciliation(
            LocalDate fyEnd,
            double fyAvg,
            List<CompanyFinancialRatiosDto> actuals,
            List<EarningsEstimateDto> estimates
    ) {
        int actualCount = actuals.size();
        int estCount = estimates.size();
        int totalQuarters = actualCount + estCount;
        if (totalQuarters == 0) {
            return new EpsReconciliationResult(false, null, null);
        }
        if (totalQuarters < 4) {
            String note = String.format(Locale.ROOT,
                    "Only %d of 4 quarters available for FY ending %s; full-year consensus (%.2f) is used directly.",
                    totalQuarters, fyEnd, fyAvg);
            return new EpsReconciliationResult(false, "INCOMPLETE_QUARTERS", note);
        }
        double actualSum = actuals.stream().mapToDouble(CompanyFinancialRatiosDto::earningsPerShare).sum();
        double estSum = estimates.stream().map(EarningsEstimateDto::epsEstimateAverage).mapToDouble(BigDecimal::doubleValue).sum();
        return buildFourQuarterReconciliationVerdict(fyAvg, actualCount, actualSum, estCount, estSum);
    }

    private EpsReconciliationResult buildFourQuarterReconciliationVerdict(
            double fyAvg,
            int actualCount,
            double actualSum,
            int estCount,
            double estSum
    ) {
        double combinedSum = actualSum + estSum;
        double diffPct = (Math.abs(combinedSum - fyAvg) / fyAvg) * 100.0;
        if (diffPct <= 5.0 || (actualCount == 0 && diffPct <= 8.0)) {
            return new EpsReconciliationResult(false, "ALIGNED", null);
        }
        if (actualCount > 0 && diffPct <= 15.0) {
            String note = String.format(Locale.ROOT,
                    "Combined %dQ GAAP actuals (%.2f) + %dQ consensus estimates (%.2f) = %.2f vs. FY consensus %.2f (%.1f%% GAAP/non-GAAP basis difference; forward P/E is reliable).",
                    actualCount, actualSum, estCount, estSum, combinedSum, fyAvg, diffPct);
            return new EpsReconciliationResult(false, "GAAP_NON_GAAP_BASIS_DIFF", note);
        }
        String note = String.format(Locale.ROOT,
                "Quarterly sum (%.2f) deviates by %.1f%% from FY consensus (%.2f); verify GAAP actual vs non-GAAP consensus alignment.",
                combinedSum, diffPct, fyAvg);
        return new EpsReconciliationResult(true, "MATERIAL_DIVERGENCE", note);
    }
}
