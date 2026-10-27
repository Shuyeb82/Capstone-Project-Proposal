package com.labbooking.ui;

import com.labbooking.Main;
import com.labbooking.dao.UserDao;
import com.labbooking.model.User;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

// লগইন স্ক্রিন। FXML ফাইল ছাড়া শুধু Java কোডে — এক স্ক্রিন = এক ফাইল।
// VBox extend করায় ক্লাসটা নিজেই একটা UI কনটেইনার, আলাদা build() লাগে না।
public class LoginView extends VBox {

    public LoginView() {
        UserDao users = new UserDao();

        TextField email = new TextField();
        PasswordField password = new PasswordField();
        email.setMaxWidth(260);
        password.setMaxWidth(260);

        Label error = new Label();
        error.setStyle("-fx-text-fill: red;");
        error.setWrapText(true);
        error.setMaxWidth(260);

        Button login = new Button("Login");
        login.setOnAction(e -> {
            try {
                User u = users.authenticate(email.getText(), password.getText());
                if (u == null) {
                    // ইমেইল ভুল না পাসওয়ার্ড ভুল — আলাদা করে বলা হয় না (DAO তেও একই কারণ)।
                    error.setText("Invalid email or password");
                    return;
                }
                Main.enter(u);
            } catch (SQLException ex) {
                error.setText("Database error: " + ex.getMessage());
            }
        });
        // পাসওয়ার্ড ঘরে Enter চাপলেও একই কাজ হবে।
        password.setOnAction(login.getOnAction());

        Button goRegister = new Button("Create an account");
        goRegister.setOnAction(e -> Main.show(new RegisterView()));

        setSpacing(8);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        // Label গুলো শুধু সাজানোর জন্য নয় — promptText স্ক্রিন রিডার ঠিকভাবে পড়ে না।
        getChildren().addAll(
                new Label("Login"),
                new Label("Email"), email,
                new Label("Password"), password,
                login, error, goRegister);
    }
}
