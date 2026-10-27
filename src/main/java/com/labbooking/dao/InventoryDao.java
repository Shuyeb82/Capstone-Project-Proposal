package com.labbooking.dao;

import com.labbooking.Database;
import com.labbooking.model.EquipmentUnit;
import com.labbooking.model.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

// ক্যাটালগ (materials) আর আসল যন্ত্র (equipment_units) — স্টকের সব কাজ।
public class InventoryDao {

    // সাবজেক্ট ড্রপডাউনের জন্য। DISTINCT = একই সাবজেক্ট একবারই আসবে।
    public List<String> listSubjects() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT DISTINCT subject FROM materials ORDER BY subject");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    // subject = null বা ফাঁকা দিলে পুরো ল্যাবের স্টক ("Show all lab stock")।
    public List<Material> listMaterials(String subject) throws SQLException {
        String filter = subject == null ? "" : subject.trim();
        // LEFT JOIN: যে material এর একটাও ইউনিট নেই সেটাও তালিকায় থাকবে (count 0)।
        // COUNT(CASE ...) শুধু AVAILABLE ইউনিট গোনে — BOOKED/ISSUED/DAMAGED বাদ।
        String sql = """
                SELECT m.id, m.subject, m.name, m.variant,
                       COUNT(CASE WHEN u.status = 'AVAILABLE' THEN 1 END) AS available
                FROM materials m
                LEFT JOIN equipment_units u ON u.material_id = m.id
                WHERE ? = '' OR m.subject = ?
                GROUP BY m.id, m.subject, m.name, m.variant
                ORDER BY m.subject, m.name, m.variant
                """;
        List<Material> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            // একই মান দুইবার বসছে, কারণ প্রতিটা ? আলাদা করে দিতে হয়।
            ps.setString(1, filter);
            ps.setString(2, filter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Material(rs.getInt("id"), rs.getString("subject"),
                            rs.getString("name"), rs.getString("variant"), rs.getInt("available")));
                }
            }
        }
        return out;
    }

    // অ্যাডমিনের ইনভেন্টরি স্ক্রিন — প্রতিটা যন্ত্র আলাদা লাইনে।
    public List<EquipmentUnit> listUnits() throws SQLException {
        String sql = """
                SELECT u.id, u.material_id, m.name, m.variant, u.serial_no, u.status
                FROM equipment_units u
                JOIN materials m ON m.id = u.material_id
                ORDER BY m.name, m.variant, u.serial_no
                """;
        List<EquipmentUnit> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(readUnit(rs));
        }
        return out;
    }

    public void addUnit(int materialId, String serialNo) throws SQLException {
        if (serialNo == null || serialNo.isBlank()) {
            throw new IllegalArgumentException("Serial number is required");
        }
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO equipment_units (material_id, serial_no) VALUES (?, ?)")) {
            ps.setInt(1, materialId);
            ps.setString(2, serialNo.trim());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Serial number " + serialNo.trim() + " already exists");
        }
    }

    // নষ্ট যন্ত্র সারানোর পর AVAILABLE করা, বা হাতে DAMAGED চিহ্নিত করা।
    public void setUnitStatus(int unitId, String status) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE equipment_units SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, unitId);
            ps.executeUpdate();
        }
    }

    // ResultSet এর এক row থেকে অবজেক্ট বানানো — তিন জায়গায় একই কোড লিখতে হয় না।
    static EquipmentUnit readUnit(ResultSet rs) throws SQLException {
        return new EquipmentUnit(rs.getInt("id"), rs.getInt("material_id"),
                Material.label(rs.getString("name"), rs.getString("variant")),
                rs.getString("serial_no"), rs.getString("status"));
    }
}
