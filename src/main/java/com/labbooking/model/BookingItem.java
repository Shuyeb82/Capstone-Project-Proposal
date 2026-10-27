package com.labbooking.model;

// রিকোয়েস্টের ভেতরের এক লাইন: কোন material কত পিস, আর ওই লাইনের স্ট্যাটাস।
// অ্যাডমিন চাইলে পুরো ব্যাচ নয়, এক লাইনও approve/reject করতে পারে — তাই
// status টা request এর পাশাপাশি এখানেও আলাদা করে রাখা।
public class BookingItem {

    private final int id;
    private final int requestId;
    private final int materialId;
    private final String materialName;
    private final int quantity;
    private final String status;

    public BookingItem(int id, int requestId, int materialId, String materialName, int quantity, String status) {
        this.id = id;
        this.requestId = requestId;
        this.materialId = materialId;
        this.materialName = materialName;
        this.quantity = quantity;
        this.status = status;
    }

    public int getId() { return id; }
    public int getRequestId() { return requestId; }
    public int getMaterialId() { return materialId; }
    public String getMaterialName() { return materialName; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
}
