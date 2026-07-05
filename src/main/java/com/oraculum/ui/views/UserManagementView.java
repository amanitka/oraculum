package com.oraculum.ui.views;

import com.oraculum.user.api.UserManagementApi;
import com.oraculum.user.api.domain.Role;
import com.oraculum.user.api.dto.UserDto;
import com.oraculum.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("User Management | Oraculum")
@RolesAllowed("ADMIN")
public class UserManagementView extends VerticalLayout {

    private final UserManagementApi userManagementApi;
    private final Grid<UserDto> grid;

    public UserManagementView(UserManagementApi userManagementApi) {
        this.userManagementApi = userManagementApi;

        setSpacing(true);
        setPadding(false);

        H2 title = new H2("User Management");

        Button addUserButton = new Button("Add User", _ -> openUserDialog(null));
        
        HorizontalLayout headerLayout = new HorizontalLayout(title, addUserButton);
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

        grid = new Grid<>(UserDto.class, false);
        grid.addColumn(UserDto::email).setHeader("Email");
        grid.addColumn(UserDto::displayName).setHeader("Name");
        grid.addColumn(UserDto::provider).setHeader("Provider");
        grid.addColumn(UserDto::role).setHeader("Role");
        grid.addColumn(UserDto::analysisLimit).setHeader("Limit");
        grid.addColumn(UserDto::enabled).setHeader("Enabled");
        grid.addColumn(UserDto::lastLoginAt).setHeader("Last Login");
        grid.addComponentColumn(user -> {
            Button editButton = new Button("Edit", _ -> openUserDialog(user));
            editButton.getStyle().set("cursor", "pointer");
            return editButton;
        }).setHeader("Actions");

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
