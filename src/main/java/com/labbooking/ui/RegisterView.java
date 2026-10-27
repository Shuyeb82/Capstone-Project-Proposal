package com.labbooking.ui;

import com.labbooking.Main;
import com.labbooking.dao.UserDao;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

// নতুন STUDENT অ্যাকাউন্ট খোলার স্ক্রিন। role এখানে পাঠানো হয় না —
// UserDao.register সবসময় 'STUDENT' বসায়, তাই UI দিয়ে admin বানানো সম্ভব নয়।
public class RegisterView extends VBox {

    public RegisterView() {
        UserDao users = new UserDao();

        TextField fullName = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        fullName.setMaxWidth(260);
        email.setMaxWidth(260);
        password.setMaxWidth(260);

        Label error = new Label();
        error.setStyle("-fx-text-fill: red;");
        error.setWrapText(true);
        error.setMaxWidth(260);

        Button register = new Button("Register");
        register.setOnAction(e -> {
            try {
                // নাম/ইমেইল/পাসওয়ার্ডের নিয়ম আর ডুপ্লিকেট ইমেইল — সব চেক DAO তে আছে।
                // এখানে শুধু সেই মেসেজটা দেখানো হয়, নিয়ম দুইবার লেখা হয় না।
                Main.enter(users.register(fullName.getText(), email.getText(), password.getText()));
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
            } catch (SQLException ex) {
                error.setText("Database error: " + ex.getMessage());
            }
        });
        password.setOnAction(register.getOnAction());

        Button goLogin = new Button("Back to login");
        goLogin.setOnAction(e -> Main.show(new LoginView()));

        setSpacing(8);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        getChildren().addAll(
                new Label("Create a student account"),
                new Label("Full name"), fullName,
                new Label("Email"), email,
                new Label("Password (at least 8 characters)"), password,
                register, error, goLogin);
    }
}
