package com.labbooking.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// একটা সাবমিট করা রিকোয়েস্টের হেডার। ভেতরের লাইনগুলো BookingItem এ।
public class BookingRequest {

    private final int id;
    private final String requestCode;
    private final int studentId;
    // অ্যাডমিনের pending-তালিকায় কার রিকোয়েস্ট সেটা নাম দিয়েই বোঝা দরকার।
    private final String studentName;
    private final String purpose;
    // DATE কলামের জন্য LocalDate, DATETIME এর জন্য LocalDateTime —
    // পুরোনো java.util.Date ব্যবহার করা হয়নি, ওটার API বিভ্রান্তিকর।
    private final LocalDate returnDate;
    private final String status;
    private final LocalDateTime createdAt;

    public BookingRequest(int id, String requestCode, int studentId, String studentName,
                          String purpose, LocalDate returnDate, String status, LocalDateTime createdAt) {
        this.id = id;
        this.requestCode = requestCode;
        this.studentId = studentId;
        this.studentName = studentName;
        this.purpose = purpose;
        this.returnDate = returnDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getRequestCode() { return requestCode; }
    public int getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getPurpose() { return purpose; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
