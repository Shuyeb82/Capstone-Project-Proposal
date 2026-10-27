package com.labbooking.model;

// একটা আসল যন্ত্র — যার গায়ে সিরিয়াল নম্বর লেখা আছে।
// materialName টেবিলের কলাম নয়, JOIN থেকে আসে; ইনভেন্টরি টেবিলে
// শুধু material_id (একটা সংখ্যা) দেখালে অ্যাডমিন কিছুই বুঝবে না।
public class EquipmentUnit {

    private final int id;
    private final int materialId;
    private final String materialName;
    private final String serialNo;
    // "AVAILABLE" | "BOOKED" | "ISSUED" | "DAMAGED"
    private final String status;

    public EquipmentUnit(int id, int materialId, String materialName, String serialNo, String status) {
        this.id = id;
        this.materialId = materialId;
        this.materialName = materialName;
        this.serialNo = serialNo;
        this.status = status;
    }

    public int getId() { return id; }
    public int getMaterialId() { return materialId; }
    public String getMaterialName() { return materialName; }
    public String getSerialNo() { return serialNo; }
    public String getStatus() { return status; }
}
