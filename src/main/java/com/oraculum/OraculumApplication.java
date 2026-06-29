package com.oraculum;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
@Push
@ColorScheme(ColorScheme.Value.LIGHT_DARK)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("context://styles.css")
public class OraculumApplication implements AppShellConfigurator {

    static void main(String[] args) {
        SpringApplication.run(OraculumApplication.class, args);
    }
}
