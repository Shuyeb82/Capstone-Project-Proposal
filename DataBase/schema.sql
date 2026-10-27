-- ICT Lab Resource Booking System — স্কিমা (MySQL 8 / MariaDB)
-- চালানোর নিয়ম:  mysql -u root < db/schema.sql

CREATE DATABASE IF NOT EXISTS lab_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_booking;

-- বার বার চালালেও যেন পরিষ্কার শুরু হয়। আগে child টেবিল, পরে parent —
-- নাহলে foreign key এর কারণে DROP আটকে যায়।
DROP TABLE IF EXISTS penalties;
DROP TABLE IF EXISTS booking_items;
DROP TABLE IF EXISTS equipment_units;
DROP TABLE IF EXISTS booking_requests;
DROP TABLE IF EXISTS materials;
DROP TABLE IF EXISTS users;

-- ছাত্র আর অ্যাডমিন — দুই ধরনের ইউজার একই টেবিলে, role দিয়ে আলাদা।
CREATE TABLE users (
    -- AUTO_INCREMENT = নতুন row ঢোকালে id নিজে নিজে বাড়ে, আমাদের দিতে হয় না।
    id            INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    -- UNIQUE: একই ইমেইলে দুইবার রেজিস্টার করা DB নিজেই আটকে দেবে।
    email         VARCHAR(190) NOT NULL UNIQUE,
    -- প্লেইন পাসওয়ার্ড কখনো রাখা হয় না। ফরম্যাট: saltHex$sha256Hex(saltHex + password)
    password_hash VARCHAR(100) NOT NULL,
    -- ENUM = C++ এর enum এর মতো, এই দুইটার বাইরে কিছু ঢুকবে না।
    role          ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT'
);

-- ক্যাটালগ: "কী ধরনের জিনিস আছে" — আসল যন্ত্র নয়, শুধু বর্ণনা।
CREATE TABLE materials (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    subject VARCHAR(80) NOT NULL,
    name    VARCHAR(80) NOT NULL,
    -- variant খালি থাকলে NULL নয় '' রাখছি — MySQL এ NULL মান UNIQUE নিয়ম মানে না,
    -- তাই NULL দিলে একই (subject,name) দুইবার ঢোকানো যেত।
    variant VARCHAR(80) NOT NULL DEFAULT '',
    UNIQUE (subject, name, variant)
);

-- একটা সাবমিট করা রিকোয়েস্ট (পুরো ব্যাচের হেডার)।
CREATE TABLE booking_requests (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    -- একসাথে সাবমিট করা সবকিছু একই request_code শেয়ার করে (README এর নিয়ম)।
    request_code VARCHAR(20) NOT NULL,
    student_id   INT NOT NULL,
    purpose      VARCHAR(255) NOT NULL,
    return_date  DATE NOT NULL,
    status       ENUM('PENDING','APPROVED','REJECTED','ISSUED','RETURNED') NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- FOREIGN KEY = এই কলামের মান অন্য টেবিলে থাকতেই হবে। ছাত্র মুছলে তার
    -- রিকোয়েস্টও মুছে যাবে, নাহলে মালিকহীন row পড়ে থাকত।
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX (request_code)
);

-- প্রতিটা আসল যন্ত্রের নিজের row — তিনটা ব্রেডবোর্ড মানে তিনটা row।
CREATE TABLE equipment_units (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    material_id INT NOT NULL,
    serial_no   VARCHAR(50) NOT NULL UNIQUE,
    status      ENUM('AVAILABLE','BOOKED','ISSUED','DAMAGED') NOT NULL DEFAULT 'AVAILABLE',
    -- এই মুহূর্তে কোন রিকোয়েস্ট ইউনিটটা আটকে রেখেছে। ফেরত নেওয়ার সময়
    -- "এই রিকোয়েস্টের কোন কোন ইউনিট" খুঁজতে এটা ছাড়া উপায় নেই; ফেরত দিলে আবার NULL।
    request_id  INT NULL,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE CASCADE,
    FOREIGN KEY (request_id) REFERENCES booking_requests(id) ON DELETE SET NULL,
    INDEX (material_id, status)
);

-- রিকোয়েস্টের ভেতরের এক লাইন: কোন material কত পিস।
CREATE TABLE booking_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    request_id  INT NOT NULL,
    material_id INT NOT NULL,
    quantity    INT NOT NULL,
    -- অ্যাডমিন পুরো ব্যাচ নয়, আলাদা আলাদা আইটেমও approve/reject করতে পারে।
    status      ENUM('PENDING','APPROVED','REJECTED','ISSUED','RETURNED') NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (request_id) REFERENCES booking_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE CASCADE,
    -- CHECK: quantity 0 বা মাইনাস হলে DB নিজেই reject করবে।
    CHECK (quantity > 0),
    UNIQUE (request_id, material_id)
);

-- দেরিতে ফেরত বা নষ্ট যন্ত্রের জরিমানা।
CREATE TABLE penalties (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL,
    reason     VARCHAR(255) NOT NULL,
    -- টাকার হিসাবে DECIMAL, কখনো FLOAT/DOUBLE নয় — ভগ্নাংশে গোলমাল হয়।
    amount     DECIMAL(8,2) NOT NULL,
    paid       BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (request_id) REFERENCES booking_requests(id) ON DELETE CASCADE
);
