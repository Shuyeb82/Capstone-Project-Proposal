package com.labbooking;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

// ডাটাবেস কানেকশনের জন্য একটাই জায়গা। পুরো অ্যাপ এখান থেকেই Connection নেবে।
public class Database {

    // static final = C++ এর static const এর মতো, কিন্তু মান রানটাইমে বের করা হয়।
    // ক্লাসটা প্রথমবার ব্যবহার হওয়ার সময় load() একবারই চলে, তারপর আর না।
    private static final Properties CONFIG = load();

    private static final String URL = CONFIG.getProperty("url")
            + (CONFIG.getProperty("url").contains("?") ? "&" : "?")
            + "createDatabaseIfNotExist=true&allowMultiQueries=true";

    private static Properties load() {
        // try-with-resources: ব্লক শেষ হলে in.close() অটো হয়ে যায় (C++ এর RAII/destructor এর মতো)।
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            // getResourceAsStream ফাইল খোঁজে classpath এ (target/classes বা jar এর ভেতরে),
            // ফোল্ডার পাথ দিয়ে নয়। ফাইল না থাকলে exception ছোড়ে না, শুধু null দেয়।
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found - copy src/main/resources/db.properties.example to db.properties and fill it in");
            }
            Properties p = new Properties();
            // key=value ফরম্যাটের ফাইল পড়ে নেয়।
            p.load(in);
            return p;
        } catch (IOException e) {
            // মূল exception টা e হিসেবে সাথে দিচ্ছি, নাহলে আসল কারণ হারিয়ে যায়।
            throw new IllegalStateException("Cannot read db.properties", e);
        }
    }

    // throws SQLException = checked exception: যে কল করবে তাকে হয় catch করতে হবে,
    // নয়তো নিজেও throws লিখতে হবে। C++ এ এই বাধ্যবাধকতা নেই।
    public static Connection getConnection() throws SQLException {
        // Connection ব্যবহারের পর বন্ধ করা জরুরি — কল করার সময় try-with-resources ব্যবহার করবে।
        return DriverManager.getConnection(
                URL, CONFIG.getProperty("user"), CONFIG.getProperty("password"));
    }

    // shortcut: .sql ফাইল প্রজেক্ট ফোল্ডার থেকে পড়া, classpath থেকে নয় — jar বানালে বদলাতে হবে।
    public static void seed() throws SQLException, IOException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute(Files.readString(Path.of("db/schema.sql")));
            st.execute(Files.readString(Path.of("db/seed.sql")));
            System.out.println("Database reset from db/schema.sql + db/seed.sql");
        }
    }
}
