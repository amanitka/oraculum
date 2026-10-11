package com.oraculum.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Compact quarter navigation widget: [ ◀ ] [ Q2 2026 ▾ ] [ ▶ ]
 * Scales gracefully across multiple years of historical filings.
 */
public class QuarterStepperComponent extends HorizontalLayout {

    private final List<LocalDate> chronologicalPeriods;
    private final Consumer<LocalDate> onPeriodSelected;
    private final Button prevBtn;
    private final Button nextBtn;
    private final ComboBox<LocalDate> periodSelect;
    @Getter
    private LocalDate selectedPeriod;

    public QuarterStepperComponent(List<LocalDate> availablePeriods,
                                   LocalDate defaultPeriod,
                                   Consumer<LocalDate> onPeriodSelected) {
        this.onPeriodSelected = onPeriodSelected;
        this.chronologicalPeriods = sortChronologically(availablePeriods);
        this.selectedPeriod = resolveDefaultPeriod(defaultPeriod);

        this.prevBtn = createStepButton(VaadinIcon.CHEVRON_LEFT, "Previous quarter", -1);
        this.nextBtn = createStepButton(VaadinIcon.CHEVRON_RIGHT, "Next quarter", 1);
        this.periodSelect = createPeriodSelect(availablePeriods);

        initLayout();
        updateButtonStates();
    }

    public static String formatQuarter(LocalDate period) {
        if (period == null) return "N/A";
        int q = (period.getMonthValue() - 1) / 3 + 1;
        return "Q" + q + " " + period.getYear();
    }

    private List<LocalDate> sortChronologically(List<LocalDate> periods) {
        List<LocalDate> sorted = new ArrayList<>(periods != null ? periods : List.of());
        Collections.sort(sorted);
        return sorted;
    }

    private LocalDate resolveDefaultPeriod(LocalDate defaultPeriod) {
        if (defaultPeriod != null && chronologicalPeriods.contains(defaultPeriod)) {
            return defaultPeriod;
        }
        return chronologicalPeriods.isEmpty() ? null : chronologicalPeriods.getLast();
    }

    private void initLayout() {
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(false);
        getStyle().set("gap", "4px");
        add(prevBtn, periodSelect, nextBtn);
    }

    private Button createStepButton(VaadinIcon icon, String tooltip, int direction) {
        Button btn = new Button(icon.create());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btn.setTooltipText(tooltip);
        btn.addClickListener(_ -> step(direction));
        return btn;
    }

    private ComboBox<LocalDate> createPeriodSelect(List<LocalDate> rawPeriods) {
        ComboBox<LocalDate> select = new ComboBox<>();
        select.setWidth("135px");
        select.setClearButtonVisible(false);
        select.setAllowCustomValue(false);
        select.setItemLabelGenerator(QuarterStepperComponent::formatQuarter);

        // Display in reverse chronological order (newest first) in dropdown
        List<LocalDate> desc = new ArrayList<>(chronologicalPeriods);
        Collections.reverse(desc);
        select.setItems(desc);

        if (selectedPeriod != null) {
            select.setValue(selectedPeriod);
        }

        select.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null) {
                setSelectedPeriod(e.getValue());
            }
        });
        return select;
    }

    private void step(int direction) {
        if (chronologicalPeriods.isEmpty() || selectedPeriod == null) return;
        int currentIndex = chronologicalPeriods.indexOf(selectedPeriod);
        if (currentIndex < 0) return;

        int targetIndex = currentIndex + direction;
        if (targetIndex >= 0 && targetIndex < chronologicalPeriods.size()) {
            setSelectedPeriod(chronologicalPeriods.get(targetIndex));
        }
    }

    private void setSelectedPeriod(LocalDate period) {
        if (!Objects.equals(this.selectedPeriod, period)) {
            this.selectedPeriod = period;
            periodSelect.setValue(period);
            updateButtonStates();
            if (onPeriodSelected != null) {
                onPeriodSelected.accept(period);
            }
        }
    }

    private void updateButtonStates() {
        if (chronologicalPeriods.isEmpty() || selectedPeriod == null) {
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            return;
        }
        int index = chronologicalPeriods.indexOf(selectedPeriod);
        prevBtn.setEnabled(index > 0);
        nextBtn.setEnabled(index < chronologicalPeriods.size() - 1);
    }

}
