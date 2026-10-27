package com.labbooking.dao;

import com.labbooking.Database;
import com.labbooking.model.BookingItem;
import com.labbooking.model.BookingRequest;
import com.labbooking.model.EquipmentUnit;
import com.labbooking.model.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// রিকোয়েস্টের পুরো জীবনচক্র: submit → approve/reject → issue → return।
public class BookingDao {

    // একই material আবার যোগ করলে নতুন লাইন নয়, আগের লাইনের quantity বাড়ে।
    // নিয়মটা UI তে নয় এখানে, কারণ JavaFX চালু না করেই এটা টেস্ট করা যায়।
    // BookingItem immutable, তাই লাইন বদলানো মানে নতুন লাইন বসিয়ে দেওয়া।
    public static void addToCart(List<BookingItem> cart, Material material, int quantity) {
        for (int i = 0; i < cart.size(); i++) {
            BookingItem line = cart.get(i);
            if (line.getMaterialId() == material.getId()) {
                cart.set(i, new BookingItem(0, 0, material.getId(), line.getMaterialName(),
                        line.getQuantity() + quantity, "PENDING"));
                return;
            }
        }
        cart.add(new BookingItem(0, 0, material.getId(), material.toString(), quantity, "PENDING"));
    }

    // কার্টের এক লাইন BookingItem দিয়েই বোঝানো হচ্ছে (id/request_id তখন 0),
    // যাতে কার্টের জন্য আলাদা ক্লাস না লাগে আর এরর মেসেজে জিনিসের নামও পাওয়া যায়।
    public String submit(int studentId, String purpose, LocalDate returnDate, List<BookingItem> cart)
            throws SQLException {
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Purpose is required");
        }
        // আজকের তারিখও চলবে না — ফেরতের দিন ভবিষ্যতে হতে হবে।
        if (returnDate == null || !returnDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Return date must be in the future");
        }
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Add at least one item to the request");
        }

        String code = "REQ-" + DateTimeFormatter.ofPattern("yyMMddHHmmss").format(java.time.LocalDateTime.now());
        Connection c = Database.getConnection();
        try {
            // autoCommit বন্ধ করলে commit() ডাকার আগ পর্যন্ত কিছুই পাকা হয় না।
            // README এর নিয়ম: স্টক কম পড়লে পুরোটা বাতিল, অর্ধেক সেভ নয়।
            c.setAutoCommit(false);

            int requestId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO booking_requests (request_code, student_id, purpose, return_date) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, code);
                ps.setInt(2, studentId);
                ps.setString(3, purpose.trim());
                ps.setObject(4, returnDate);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    requestId = keys.getInt(1);
                }
            }

            for (BookingItem line : cart) {
                if (line.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity must be at least 1");
                }
                // কার্টে যা দেখানো হয়েছিল সেই স্টক এখনো আছে কি না — এখন আবার দেখা হচ্ছে,
                // কারণ মাঝের সময়ে অন্য ছাত্র নিয়ে ফেলতে পারে। FOR UPDATE ওই row গুলো
                // লক করে রাখে, তাই commit এর আগে আর কেউ একই ইউনিট নিতে পারবে না।
                List<Integer> unitIds = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id FROM equipment_units WHERE material_id = ? AND status = 'AVAILABLE' "
                                + "ORDER BY id LIMIT ? FOR UPDATE")) {
                    ps.setInt(1, line.getMaterialId());
                    ps.setInt(2, line.getQuantity());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) unitIds.add(rs.getInt(1));
                    }
                }
                if (unitIds.size() < line.getQuantity()) {
                    throw new IllegalStateException("Only " + unitIds.size() + " of "
                            + line.getQuantity() + " " + line.getMaterialName()
                            + " are available now. Nothing was saved.");
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO booking_items (request_id, material_id, quantity) VALUES (?, ?, ?)")) {
                    ps.setInt(1, requestId);
                    ps.setInt(2, line.getMaterialId());
                    ps.setInt(3, line.getQuantity());
                    // একই material দুইবার থাকলে UNIQUE(request_id, material_id) এখানেই আটকাবে,
                    // আর নিচের catch পুরো লেনদেন রোলব্যাক করে দেবে।
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE equipment_units SET status = 'BOOKED', request_id = ? WHERE id = ?")) {
                    for (int unitId : unitIds) {
                        ps.setInt(1, requestId);
                        ps.setInt(2, unitId);
                        ps.executeUpdate();
                    }
                }
            }

            c.commit();
            return code;
        } catch (SQLException | RuntimeException e) {
            // RuntimeException ও ধরা হচ্ছে, নাহলে স্টক কম পড়ার throw এর পর
            // আগের INSERT গুলো DB তে ঝুলে থাকত।
            c.rollback();
            throw e;
        } finally {
            // try-with-resources ব্যবহার করা যায়নি, কারণ rollback() এর জন্য
            // Connection টা catch ব্লকেও দরকার — তাই হাতে বন্ধ করছি।
            c.close();
        }
    }

    public List<BookingRequest> listByStudent(int studentId) throws SQLException {
        return listRequests("WHERE r.student_id = ?", studentId);
    }

    // অ্যাডমিনের স্ক্রিন তিন অবস্থার তালিকা চায়: PENDING (সিদ্ধান্ত),
    // APPROVED (হাতে দেওয়া), ISSUED (ফেরত নেওয়া) — তাই একটাই মেথড।
    public List<BookingRequest> listByStatus(String status) throws SQLException {
        return listRequests("WHERE r.status = ?", status);
    }

    public List<BookingItem> listItems(int requestId) throws SQLException {
        String sql = """
                SELECT i.id, i.request_id, i.material_id, m.name, m.variant, i.quantity, i.status
                FROM booking_items i
                JOIN materials m ON m.id = i.material_id
                WHERE i.request_id = ?
                ORDER BY m.name, m.variant
                """;
        List<BookingItem> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new BookingItem(rs.getInt("id"), rs.getInt("request_id"), rs.getInt("material_id"),
                            Material.label(rs.getString("name"), rs.getString("variant")),
                            rs.getInt("quantity"), rs.getString("status")));
                }
            }
        }
        return out;
    }

    // ফেরত নেওয়ার স্ক্রিনে "এই রিকোয়েস্টের হাতে থাকা যন্ত্রগুলো"।
    public List<EquipmentUnit> listUnitsForRequest(int requestId) throws SQLException {
        String sql = """
                SELECT u.id, u.material_id, m.name, m.variant, u.serial_no, u.status
                FROM equipment_units u
                JOIN materials m ON m.id = u.material_id
                WHERE u.request_id = ?
                ORDER BY m.name, u.serial_no
                """;
        List<EquipmentUnit> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(InventoryDao.readUnit(rs));
            }
        }
        return out;
    }

    // একটা আইটেম লাইন approve বা reject। reject হলে ওই লাইনের জন্য আটকে রাখা
    // ইউনিটগুলো ছেড়ে দিতে হয়, নাহলে ওগুলো চিরকাল BOOKED হয়ে বসে থাকবে।
    public void decideItem(int itemId, boolean approve) throws SQLException {
        Connection c = Database.getConnection();
        try {
            c.setAutoCommit(false);
            int requestId;
            int materialId;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT request_id, material_id FROM booking_items WHERE id = ?")) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Item not found");
                    requestId = rs.getInt(1);
                    materialId = rs.getInt(2);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE booking_items SET status = ? WHERE id = ? AND status = 'PENDING'")) {
                ps.setString(1, approve ? "APPROVED" : "REJECTED");
                ps.setInt(2, itemId);
                ps.executeUpdate();
            }
            if (!approve) freeUnits(c, requestId, materialId);
            syncRequestStatus(c, requestId);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.close();
        }
    }

    // পুরো ব্যাচ একসাথে। আগে আলাদা করে reject করা লাইন PENDING নয়, তাই অছোঁয়া থাকে।
    public void decideRequest(int requestId, boolean approve) throws SQLException {
        Connection c = Database.getConnection();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE booking_items SET status = ? WHERE request_id = ? AND status = 'PENDING'")) {
                ps.setString(1, approve ? "APPROVED" : "REJECTED");
                ps.setInt(2, requestId);
                ps.executeUpdate();
            }
            if (!approve) freeUnits(c, requestId, null);
            syncRequestStatus(c, requestId);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.close();
        }
    }

    // ছাত্র জিনিস হাতে নিয়ে গেল: আটকে রাখা ইউনিট এখন ISSUED।
    public void recordIssue(int requestId) throws SQLException {
        Connection c = Database.getConnection();
        try {
            c.setAutoCommit(false);
            update(c, "UPDATE equipment_units SET status = 'ISSUED' WHERE request_id = ? AND status = 'BOOKED'", requestId);
            update(c, "UPDATE booking_items SET status = 'ISSUED' WHERE request_id = ? AND status = 'APPROVED'", requestId);
            update(c, "UPDATE booking_requests SET status = 'ISSUED' WHERE id = ?", requestId);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.close();
        }
    }

    // ফেরত নেওয়া: অ্যাডমিন শুধু নষ্ট ইউনিটগুলোর id দেয়, বাকিগুলো আবার AVAILABLE।
    public void recordReturn(int requestId, Set<Integer> damagedUnitIds) throws SQLException {
        Connection c = Database.getConnection();
        try {
            c.setAutoCommit(false);
            if (damagedUnitIds != null && !damagedUnitIds.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE equipment_units SET status = 'DAMAGED', request_id = NULL "
                                + "WHERE id = ? AND request_id = ?")) {
                    for (int unitId : damagedUnitIds) {
                        ps.setInt(1, unitId);
                        // request_id ও মেলানো হচ্ছে, যাতে ভুল id দিলে অন্য রিকোয়েস্টের
                        // যন্ত্র নষ্ট হিসেবে চিহ্নিত না হয়ে যায়।
                        ps.setInt(2, requestId);
                        ps.executeUpdate();
                    }
                }
            }
            update(c, "UPDATE equipment_units SET status = 'AVAILABLE', request_id = NULL "
                    + "WHERE request_id = ? AND status = 'ISSUED'", requestId);
            update(c, "UPDATE booking_items SET status = 'RETURNED' WHERE request_id = ? AND status = 'ISSUED'", requestId);
            update(c, "UPDATE booking_requests SET status = 'RETURNED' WHERE id = ?", requestId);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.close();
        }
    }

    // ---- ভেতরের ছোট হেল্পার ----

    // materialId null দিলে রিকোয়েস্টের সব আটকে রাখা ইউনিট ছাড়ে।
    private static void freeUnits(Connection c, int requestId, Integer materialId) throws SQLException {
        String sql = "UPDATE equipment_units SET status = 'AVAILABLE', request_id = NULL "
                + "WHERE request_id = ? AND status = 'BOOKED'"
                + (materialId == null ? "" : " AND material_id = ?");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            if (materialId != null) ps.setInt(2, materialId);
            ps.executeUpdate();
        }
    }

    // হেডারের স্ট্যাটাস লাইনগুলো থেকে হিসাব করা হয়, হাতে লেখা হয় না —
    // তাই "সব reject কিন্তু রিকোয়েস্ট APPROVED" এমন অসঙ্গতি তৈরি হতে পারে না।
    private static void syncRequestStatus(Connection c, int requestId) throws SQLException {
        String status;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT SUM(status = 'PENDING'), SUM(status = 'APPROVED') FROM booking_items WHERE request_id = ?")) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) return; // এখনো সিদ্ধান্ত বাকি আছে
                status = rs.getInt(2) > 0 ? "APPROVED" : "REJECTED";
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE booking_requests SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }

    private static void update(Connection c, String sql, int requestId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.executeUpdate();
        }
    }

    // দুইটা তালিকার কোয়েরি একই, শুধু WHERE আলাদা। where টা কোডে লেখা ধ্রুব স্ট্রিং,
    // ইউজারের ইনপুট নয় — তাই injection এর ঝুঁকি নেই; মান সবসময় ? দিয়েই যায়।
    private List<BookingRequest> listRequests(String where, Object param) throws SQLException {
        String sql = """
                SELECT r.id, r.request_code, r.student_id, u.full_name, r.purpose,
                       r.return_date, r.status, r.created_at
                FROM booking_requests r
                JOIN users u ON u.id = r.student_id
                """ + where + " ORDER BY r.created_at DESC, r.id DESC";
        List<BookingRequest> out = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new BookingRequest(rs.getInt("id"), rs.getString("request_code"),
                            rs.getInt("student_id"), rs.getString("full_name"), rs.getString("purpose"),
                            rs.getObject("return_date", LocalDate.class), rs.getString("status"),
                            rs.getObject("created_at", java.time.LocalDateTime.class)));
                }
            }
        }
        return out;
    }
}
