package com.labbooking;

import com.labbooking.model.User;
import com.labbooking.ui.AdminView;
import com.labbooking.ui.LoginView;
import com.labbooking.ui.StudentView;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// JavaFX অ্যাপ বানাতে হলে Application ক্লাস extend করতে হয়।
// C++ এর মতো আমরা নিজে while-loop চালাই না — JavaFX নিজেই event loop চালায়,
// আর দরকার হলে আমাদের start() মেথড কল করে।
public class Main extends Application {

    // লগইন করা ইউজার = পুরো অ্যাপের সেশন। একটাই উইন্ডো, তাই static ই যথেষ্ট।
    public static User currentUser;

    private static Scene scene;

    // Stage = উইন্ডো, Scene = ওই উইন্ডোর ভেতরের পুরো কনটেন্ট।
    // @Override মানে parent ক্লাসের মেথডকে replace করছি (C++ এর virtual override এর মতো)।
    @Override
    public void start(Stage stage) {
        stage.setTitle("ICT Lab Resource Booking System");
        // new দিয়ে বানানো object delete করতে হয় না — Java এর garbage collector নিজেই মুছে দেয়।
        scene = new Scene(new LoginView(), 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    // স্ক্রিন বদলানো মানে শুধু Scene এর root বদলানো — নতুন উইন্ডো খোলার দরকার নেই।
    public static void show(Parent view) {
        scene.setRoot(view);
    }

    // লগইন বা রেজিস্টার সফল হলে এখানে আসে, তারপর role অনুযায়ী স্ক্রিন।
    public static void enter(User user) {
        currentUser = user;
        show(home(user));
    }

    public static void logout() {
        currentUser = null;
        show(new LoginView());
    }

    // role অনুযায়ী স্ক্রিন — দুইটাই নিজের ভেতরে হেডার আর Logout রাখে।
    private static Parent home(User user) {
        return user.isAdmin() ? new AdminView() : new StudentView();
    }

    public static void main(String[] args) {
        try {
            Database.seed();
        } catch (Exception e) {
            System.err.println("Could not seed the database: " + e.getMessage());
        }
        // launch() JavaFX চালু করে, তারপর উপরের start() কল করে।
        // এই লাইনটা উইন্ডো বন্ধ না হওয়া পর্যন্ত return করে না।
        launch(args);
    }
}
