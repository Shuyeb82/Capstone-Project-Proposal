package com.labbooking.model;

// শুধু ডাটা বহন করার ক্লাস (C++ এর struct এর মতো)। কোনো লজিক নেই।
// password_hash এখানে রাখা হয়নি — UI এর কোনো স্ক্রিনের ওটা লাগে না,
// আর যে ডাটা মেমরিতে ঘোরে না সেটা ভুল করে দেখিয়ে ফেলারও ভয় নেই।
public class User {

    // final = একবার বসালে আর বদলাবে না (C++ এর const member)।
    private final int id;
    private final String fullName;
    private final String email;
    // role এর মান DB এর ENUM এর মতোই "STUDENT" বা "ADMIN"।
    private final String role;

    public User(int id, String fullName, String email, String role) {
        // Java তে ফিল্ড আর প্যারামিটারের নাম এক হলে this. দিয়ে আলাদা করতে হয়।
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public boolean isAdmin() { return "ADMIN".equals(role); }
}
