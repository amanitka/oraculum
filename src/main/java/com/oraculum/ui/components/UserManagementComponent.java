package com.oraculum.ui.components;

import com.oraculum.user.api.UserManagementApi;
import com.oraculum.user.api.domain.Role;
import com.oraculum.user.api.dto.UserDto;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import java.time.format.DateTimeFormatter;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.EmailField;

public class UserManagementComponent extends VerticalLayout {

    private final UserManagementApi userManagementApi;
    private final Grid<UserDto> grid;

    public UserManagementComponent(UserManagementApi userManagementApi) {
        this.userManagementApi = userManagementApi;

        setSpacing(true);
        setPadding(false);

        H2 title = new H2("User Management");

        Button addUserButton = new Button("Add User", _ -> openUserDialog(null));
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        
        HorizontalLayout headerLayout = new HorizontalLayout(title, addUserButton);
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        setSizeFull();

        grid = new Grid<>(UserDto.class, false);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassName("screener-grid");

        grid.addColumn(UserDto::email).setHeader("Email").setAutoWidth(true).setSortable(true);
        grid.addColumn(UserDto::displayName).setHeader("Name").setAutoWidth(true).setSortable(true);
        grid.addColumn(UserDto::provider).setHeader("Provider").setAutoWidth(true).setSortable(true);
        grid.addColumn(UserDto::role).setHeader("Role").setAutoWidth(true).setSortable(true);
        grid.addColumn(UserDto::analysisLimit).setHeader("Limit").setAutoWidth(true).setSortable(true);
        
        grid.addComponentColumn(user -> {
            Checkbox cb = new Checkbox();
            cb.setValue(user.enabled());
            cb.setReadOnly(true);
            return cb;
        }).setHeader("Enabled").setAutoWidth(true).setSortable(true);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        grid.addColumn(user -> user.lastLoginAt() != null ? user.lastLoginAt().format(formatter) : "-")
                .setHeader("Last Login").setAutoWidth(true).setSortable(true);

        grid.addComponentColumn(user -> {
            Button editButton = new Button("Edit", _ -> openUserDialog(user));
            editButton.getStyle().set("cursor", "pointer");
            editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            return editButton;
        }).setHeader("Action").setAutoWidth(true);

        add(headerLayout, grid);

        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(userManagementApi.getAllUsers());
    }

    private void openUserDialog(UserDto existingUser) {
        Dialog dialog = new Dialog();
        boolean isNew = existingUser == null;
        dialog.setHeaderTitle(isNew ? "Create New User" : "Edit User");

        EmailField emailField = new EmailField("Email Address");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        TextField firstNameField = new TextField("First Name");
        firstNameField.setWidthFull();

        TextField lastNameField = new TextField("Last Name");
        lastNameField.setWidthFull();

        ComboBox<String> roleField = new ComboBox<>("Role");
        roleField.setItems("USER", "ADMIN");
        roleField.setWidthFull();
        roleField.setValue("USER");

        Checkbox enabledField = new Checkbox("Enabled");
        enabledField.setValue(true);

        if (!isNew) {
            emailField.setValue(existingUser.email() != null ? existingUser.email() : "");
            emailField.setReadOnly(true);
            firstNameField.setValue(existingUser.firstName() != null ? existingUser.firstName() : "");
            lastNameField.setValue(existingUser.lastName() != null ? existingUser.lastName() : "");
            roleField.setValue(existingUser.role() != null ? existingUser.role() : Role.USER.name());
            enabledField.setValue(existingUser.enabled());
        }

        VerticalLayout layout = new VerticalLayout(emailField, firstNameField, lastNameField, roleField, enabledField);
        layout.setPadding(false);

        Button saveButton = new Button("Save", _ -> {
            if (emailField.isEmpty() || emailField.isInvalid()) {
                Notification.show("Please enter a valid email address.", 3000, Notification.Position.MIDDLE);
                return;
            }
            try {
                UserDto saveDto = new UserDto(
                        isNew ? null : existingUser.id(),
                        emailField.getValue(),
                        firstNameField.getValue(),
                        lastNameField.getValue(),
                        null,
                        isNew ? null : existingUser.provider(),
                        roleField.getValue(),
                        isNew ? null : existingUser.analysisLimit(),
                        enabledField.getValue(),
                        isNew ? null : existingUser.lastLoginAt()
                );
                userManagementApi.createOrUpdateUser(saveDto);
                refreshGrid();
                dialog.close();
                Notification.show("User " + (isNew ? "created" : "updated") + " successfully.", 3000, Notification.Position.BOTTOM_END);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        saveButton.getStyle().set("background-color", "var(--lumo-primary-color)");
        saveButton.getStyle().set("color", "white");

        Button cancelButton = new Button("Cancel", _ -> dialog.close());

        dialog.add(layout);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }
}
