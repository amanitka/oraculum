package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.ui.ViewHelper;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Utility for rendering badges/components directly from typed {@link AnalysisResult} objects.
 */
public final class AnalysisResultFieldHelper {

    private AnalysisResultFieldHelper() {
    }

    public static Span outlookBadge(AnalysisResult result) {
        if (result == null || result.outlook() == null) {
            return new Span("-");
        }
        return ViewHelper.outlookBadge(result.outlook());
    }

    public static Span valuationBadge(AnalysisResult result) {
        if (result == null || result.valuation() == null) {
            return new Span("-");
        }
        return ViewHelper.valuationBadge(result.valuation());
    }

    public static Span convictionSpan(AnalysisResult result) {
        if (result == null) {
            return new Span("-");
        }
        Span s = new Span(result.conviction() + "/5");
        s.addClassName(LumoUtility.FontWeight.BOLD);
        return s;
    }
}
