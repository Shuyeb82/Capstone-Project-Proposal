package com.labbooking.model;

// ক্যাটালগের এক লাইন: "কী জিনিস" + এই মুহূর্তে কয়টা খালি আছে।
// availableCount টেবিলে নেই, DAO এর COUNT() কোয়েরি থেকে আসে — তাই এটা
// সেই মুহূর্তের ছবি, পরে বদলে যেতে পারে।
public class Material {

    private final int id;
    private final String subject;
    private final String name;
    private final String variant;
    private final int availableCount;

    public Material(int id, String subject, String name, String variant, int availableCount) {
        this.id = id;
        this.subject = subject;
        this.name = name;
        this.variant = variant;
        this.availableCount = availableCount;
    }

    public int getId() { return id; }
    public String getSubject() { return subject; }
    public String getName() { return name; }
    public String getVariant() { return variant; }
    public int getAvailableCount() { return availableCount; }

    // "Breadboard (830 points)" — ইউনিট আর আইটেমের তালিকাতেও একই লেখা দরকার,
    // তাই static করে এক জায়গায় রাখা; DAO গুলো JOIN এর কলাম দিয়ে এটাই ডাকে।
    public static String label(String name, String variant) {
        return variant == null || variant.isEmpty() ? name : name + " (" + variant + ")";
    }

    // TableView বা ComboBox এ যা দেখা যাবে। toString() ওভাররাইড না করলে
    // Java অবজেক্টের মেমরি-ঠিকানা ছাপায় (যেমন Material@1b6d3586), যেটা অর্থহীন।
    @Override
    public String toString() {
        return label(name, variant);
    }
}
