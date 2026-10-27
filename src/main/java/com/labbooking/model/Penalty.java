package com.labbooking.model;

import java.math.BigDecimal;

// দেরিতে ফেরত বা নষ্ট যন্ত্রের জরিমানা।
public class Penalty {

    private final int id;
    private final int requestId;
    private final String reason;
    // টাকার জন্য BigDecimal, double নয় — double এ 0.1+0.2 ≠ 0.3 হয়ে যায়।
    private final BigDecimal amount;
    private final boolean paid;

    public Penalty(int id, int requestId, String reason, BigDecimal amount, boolean paid) {
        this.id = id;
        this.requestId = requestId;
        this.reason = reason;
        this.amount = amount;
        this.paid = paid;
    }

    public int getId() { return id; }
    public int getRequestId() { return requestId; }
    public String getReason() { return reason; }
    public BigDecimal getAmount() { return amount; }
    public boolean isPaid() { return paid; }
}
