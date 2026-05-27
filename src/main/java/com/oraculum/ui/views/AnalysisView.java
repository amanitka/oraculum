package com.oraculum.ui.views;

import com.oraculum.ui.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route(value = "analysis", layout = MainLayout.class)
@PageTitle("Analysis | Oraculum")
public class AnalysisView extends VerticalLayout {

    public AnalysisView() {
        addClassNames(LumoUtility.Padding.MEDIUM);
        setSizeFull();

        H2 heading = new H2("Analysis");
        Paragraph placeholder = new Paragraph("Analysis features coming soon.");
        placeholder.addClassNames(LumoUtility.TextColor.SECONDARY);

        add(heading, placeholder);
    }
}
