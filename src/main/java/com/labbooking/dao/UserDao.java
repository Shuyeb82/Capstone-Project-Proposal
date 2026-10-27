package com.labbooking.dao;

import com.labbooking.Database;
import com.labbooking.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.HexFormat;

// রেজিস্টার আর লগইন — users টেবিলের সব কাজ এখানে।
public class UserDao {

    // সব কোয়েরি PreparedStatement দিয়ে, ? প্লেসহোল্ডারসহ। স্ট্রিং জোড়া দিয়ে
    // SQL বানালে ইউজারের লেখা টেক্সট কোয়েরির অংশ হয়ে যায় (SQL injection)।

    public User register(String fullName, String email, String password) throws SQLException {
        // ভ্যালিডেশন UI তে নয়, এখানে — কারণ DAO ই শেষ দরজা। UI শুধু মেসেজ দেখাবে।
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (email == null || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        String sql = "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, 'STUDENT')";
        try (Connection c = Database.getConnection();
             // RETURN_GENERATED_KEYS চাইলে AUTO_INCREMENT এ বসা নতুন id ফেরত পাওয়া যায়।
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, fullName.trim());
            ps.setString(2, email.trim());
            ps.setString(3, hash(password));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getInt(1), fullName.trim(), email.trim(), "STUDENT");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // email কলামে UNIQUE আছে, তাই ডুপ্লিকেট DB নিজেই আটকায় — সেই
            // টেকনিক্যাল এরর ইউজারের পড়ার মতো বাক্যে বদলে দিচ্ছি।
            throw new IllegalArgumentException("This email is already registered");
        }
    }

    // পাসওয়ার্ড মিললে User, না মিললে null। "ইমেইল ভুল" আর "পাসওয়ার্ড ভুল"
    // আলাদা করে বলা হয় না — তাহলে কোন ইমেইল আছে সেটা বাইরে ফাঁস হয়।
    public User authenticate(String email, String password) throws SQLException {
        String sql = "SELECT id, full_name, email, password_hash, role FROM users WHERE email = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !verify(password, rs.getString("password_hash"))) {
                    return null;
                }
                return new User(rs.getInt("id"), rs.getString("full_name"),
                        rs.getString("email"), rs.getString("role"));
            }
        }
    }

    // ফরম্যাট: saltHex$sha256Hex(saltHex + password) — seed.sql এর হ্যাশও এভাবেই বানানো।
    // salt আলাদা আলাদা হওয়ায় দুইজনের একই পাসওয়ার্ডের হ্যাশও আলাদা হয়।
    // shortcut: SHA-256 দ্রুত, তাই brute-force এ দুর্বল; আসল ডিপ্লয়মেন্টে BCrypt/Argon2 এ যাবে।
    public static String hash(String password) {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        return saltHex + "$" + sha256Hex(saltHex + password);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        int sep = stored.indexOf('$');
        if (sep < 0) return false;
        String saltHex = stored.substring(0, sep);
        // isEqual সব বাইট সমান সময়ে মেলায়, == এর মতো প্রথম অমিলেই থামে না (timing attack)।
        return MessageDigest.isEqual(
                sha256Hex(saltHex + password).getBytes(StandardCharsets.UTF_8),
                stored.substring(sep + 1).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 প্রতিটা JVM এ থাকে — এখানে আসা মানে JVM নিজেই ভাঙা।
            throw new IllegalStateException(e);
        }
    }
}
