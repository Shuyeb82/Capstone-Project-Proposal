-- ডেমো ডাটা। চালানোর নিয়ম:  mysql -u root < db/seed.sql
-- schema.sql আগে চালাতে হবে (ওটা টেবিল মুছে নতুন বানায়)।

USE lab_booking;

-- তিনটা অ্যাকাউন্টের পাসওয়ার্ড একই: password123
-- হ্যাশ ফরম্যাট: saltHex$sha256Hex(saltHex + password) — Phase 3 এর কোড এভাবেই মেলাবে।
INSERT INTO users (full_name, email, password_hash, role) VALUES
  ('Lab Administrator', 'admin@lab.edu',  'a1b2c3d4e5f60718$b90f4bebd6a802058aa94ae1342fa683358e42caa59b78f1c1fbbe854ff18c07', 'ADMIN'),
  ('Al-Shahariar ',   'shahariar@lab.edu',  '0f1e2d3c4b5a6978$431e12094b57b10756044f6b51a2a72fd32e9e4aef59ca3a5739c24d200fe271', 'STUDENT'),
  ('Nusrat Jahan',      'nusrat@lab.edu', '1122334455667788$007b6e73fb8356defe2704fb3dff1766f82838c89594d7e7d215bd068fe49cdd', 'STUDENT');

-- পুরোনো প্রজেক্টের অ্যাডমিন: admin@mbstu.ac.bd / admin123
INSERT INTO users (full_name, email, password_hash, role) VALUES
  ('Lab Assistant', 'admin@mbstu.ac.bd', '0011223344556677$ba03894baaaf61dea22244bd0eaafad14df05680339f48a5db28f0a4d79766a9', 'ADMIN');

-- একই তথ্য দিয়ে নিচে materials আর equipment_units দুইটাই ভরবে।
DROP TEMPORARY TABLE IF EXISTS seed_catalog;
CREATE TEMPORARY TABLE seed_catalog (
    subject VARCHAR(80) NOT NULL,
    name    VARCHAR(80) NOT NULL,
    variant VARCHAR(80) NOT NULL DEFAULT '',
    qty     INT NOT NULL,
    prefix  VARCHAR(20) NOT NULL
);

-- লিস্টে দুই মডেল একসাথে লেখা থাকলে (যেমন "MG996R=02, SG90=05") আলাদা variant।
INSERT INTO seed_catalog (subject, name, variant, qty, prefix) VALUES
  ('IOT Lab', 'Arduino Mega',                  '2556 (China)',        15,  'ARDM'),
  ('IOT Lab', 'Jumper Wire',                   'Single',              500, 'JWS'),
  ('IOT Lab', 'Smoke Sensor Module',           'MQ-2/MQ-5',           3,   'MQ25'),
  ('IOT Lab', 'Fingerprint Scanner',           '',                    2,   'FPS'),
  ('IOT Lab', 'Temperature Sensor Module',     '',                    5,   'TSM'),
  ('IOT Lab', 'Temperature and Humidity Sensor Module', '',           2,   'THSM'),
  ('IOT Lab', 'Temperature Sensor',            '',                    10,  'TS'),
  ('IOT Lab', 'JST SH Jumper Wire Assembly',   '4 Wire 8 inch',       1,   'JST'),
  ('IOT Lab', 'Rechargeable Battery',          '9V',                  1,   'BAT9'),
  ('IOT Lab', 'Rechargeable Battery',          '3.7V',                15,  'BAT37'),
  ('IOT Lab', 'LED',                           '',                    50,  'LED'),
  ('IOT Lab', 'Raspberry Pi',                  '3 Model B+',          2,   'RPI3'),
  ('IOT Lab', 'Raspberry Pi',                  '4 Model B 4GB',       19,  'RPI4'),
  ('IOT Lab', 'PIR Sensor Module',             'HW-416 HC-SR501',     2,   'PIR'),
  ('IOT Lab', 'FTDI USB to Serial Converter',  '3V3-5V',              2,   'FTDI'),
  ('IOT Lab', 'ESP8266 Node MCU',              'V2 Development Board', 4,  'ESP82'),
  ('IOT Lab', 'ESP32',                         '',                    5,   'ESP32'),
  ('IOT Lab', 'Digital Temperature Sensor',    'CN',                  2,   'DTS'),
  ('IOT Lab', 'LDR',                           '5mm',                 25,  'LDR'),
  ('IOT Lab', 'Servo Motor',                   'MG996R',              2,   'SRVM'),
  ('IOT Lab', 'Servo Motor',                   'SG90',                5,   'SRVS'),
  ('IOT Lab', 'Digital Buzzer Module',         '',                    2,   'BUZ'),
  ('IOT Lab', 'Relay Module',                  '1 Channel 12V',       5,   'RLY12'),
  ('IOT Lab', 'Relay Module',                  '1 Channel 5V',        5,   'RLY5'),
  ('IOT Lab', 'Dust Sensor',                   '',                    2,   'DUST'),
  ('IOT Lab', 'Heart Rate Monitor Sensor',     'Gravity Analog ECG',  2,   'ECG'),
  ('IOT Lab', 'PH Sensor Meter Kit',           'Gravity Analog V2',   1,   'PH'),
  ('IOT Lab', 'Moisture Sensor',               'Grove',               2,   'MOIS'),
  ('IOT Lab', 'Breadboard Power Supply Stick', '3.3V/5V',             5,   'BPS'),
  ('IOT Lab', 'OLED Display',                  'Blue I2C 128x64 0.96 inch', 2, 'OLED'),
  ('IOT Lab', 'LCD Display',                   '16x2',                5,   'LCD16'),
  ('IOT Lab', 'HDMI to DVI Cable',             '',                    2,   'HDVI'),
  ('IOT Lab', 'HDMI to HDMI Cable',            '',                    2,   'HHDM'),
  ('IOT Lab', 'USB Cable',                     'A to Mini B',         2,   'USBM'),
  ('IOT Lab', 'Sonar Sensor',                  '',                    5,   'SON'),
  ('IOT Lab', 'Sound Sensor',                  'Grove',               2,   'SND'),
  ('IOT Lab', 'Rain Sensor',                   '',                    2,   'RAIN'),
  ('IOT Lab', 'Carbon Monoxide Sensor',        'MQ7',                 2,   'MQ7'),
  ('IOT Lab', 'Liquid Level Sensor',           '',                    2,   'LLS'),
  ('IOT Lab', 'Obstacle Sensor',               '',                    2,   'OBS'),
  ('IOT Lab', 'IR Transmitter',                'White 5mm',           25,  'IRT'),
  ('IOT Lab', 'IR Receiver Diode',             '3mm',                 25,  'IRD'),
  ('IOT Lab', 'IR Receiver Sensor',            '',                    5,   'IRS'),
  ('IOT Lab', 'IR Receiver Shielded',          '',                    7,   'IRSH'),
  ('IOT Lab', 'Infrared Photodiode',           'Pair T&R',            7,   'IRPD'),
  ('IOT Lab', 'IR Sensor Mini',                'Pair T&R',            3,   'IRMN'),
  ('IOT Lab', 'SIM868 GSM/GPRS/GPS',           '',                    1,   'SIM868'),
  ('IOT Lab', 'Raspberry Pi TFT LCD Module',   '7 inch Display',      2,   'TFT'),
  ('IOT Lab', 'Raspberry Pi TFT LCD Touch Module', '7 inch Display',  1,   'TFTT'),
  ('IOT Lab', 'LCD Graphic Display',           '128x64',              1,   'LCDG'),
  ('IOT Lab', 'ESP8266 WiFi Smart Switch',     '4 Channel',           1,   'ESPSW'),
  ('IOT Lab', 'Raspberry Pi OS SD Card',       'Raspbian Stretch With Desktop', 2, 'RPIOS'),
  ('IOT Lab', 'Adapter Board for Arduino & Raspberry Pi', '',         2,   'ADPT'),
  ('IOT Lab', 'Arduino WiFi Shield',           'Original',            1,   'WIFISH'),
  ('IOT Lab', 'TSOP1736 IR Receiver',          '',                    2,   'TSOP36'),
  ('IOT Lab', 'TSOP1738 IR Receiver',          '',                    2,   'TSOP38'),
  ('IOT Lab', 'IR Receiver TSOP38236',         '',                    2,   'TSOP382'),
  ('IOT Lab', 'GPS Tracker',                   'Sino Track ST-901',   1,   'GPS'),
  ('IOT Lab', 'Micro HDMI to HDMI Cable',      'Male to Male',        20,  'MHDMI'),
  ('IOT Lab', 'Micro SD Card',                 'Twinmos 128GB',       15,  'SDC'),
  ('IOT Lab', 'USB Charging Adapter',          'Oramio Cannon 18S 18W', 10, 'CHG'),
  ('IOT Lab', 'Monitor',                       'HP 23.8 inch FHD IPS HDMI VGA White', 1, 'MON'),
  ('IOT Lab', 'Wireless Mini Keyboard',        'With Touchpad',       1,   'KBD'),
  -- পুরোনো database.sql এর তিনটা subject। এক material এক subject এ থাকে, তাই
  -- শেয়ার করা আইটেমের স্টক ল্যাবগুলোর মধ্যে ভাগ করা।
  ('EEE Lab',      'Breadboard',         '830 points', 4,  'EEE-BB'),
  ('EEE Lab',      'Arduino Uno',        'R3',         4,  'EEE-ARDU'),
  ('EEE Lab',      'Sensor',             'DHT11',      3,  'EEE-DHT'),
  ('EEE Lab',      'Digital Multimeter', '',           5,  'EEE-DMM'),
  ('EEE Lab',      'Soldering Iron',     '60W',        5,  'EEE-SLD'),
  ('IoT Project',  'Breadboard',         '830 points', 3,  'IOTP-BB'),
  ('IoT Project',  'Arduino Uno',        'R3',         3,  'IOTP-ARDU'),
  ('IoT Project',  'Sensor',             'DHT11',      3,  'IOTP-DHT'),
  ('IoT Project',  'Raspberry Pi',       '4 Model B 4GB', 3, 'IOTP-RPI'),
  ('Robotics Lab', 'Breadboard',         '830 points', 3,  'ROB-BB'),
  ('Robotics Lab', 'Arduino Uno',        'R3',         3,  'ROB-ARDU'),
  ('Robotics Lab', 'Sensor',             'HC-SR04',    4,  'ROB-USND'),
  ('Robotics Lab', 'Sensor',             'PIR motion', 4,  'ROB-PIR');

INSERT INTO materials (subject, name, variant)
SELECT subject, name, variant FROM seed_catalog;

-- qty যত, তত row — হাতে ৯০০+ লাইন না লিখে ০-৯৯৯ এর নাম্বার টেবিল দিয়ে JOIN।
INSERT INTO equipment_units (material_id, serial_no, status)
SELECT m.id,
       CONCAT(c.prefix, '-', LPAD(n.n + 1, 3, '0')),
       'AVAILABLE'
FROM seed_catalog c
JOIN materials m ON m.subject = c.subject AND m.name = c.name AND m.variant = c.variant
JOIN (
    SELECT d2.d * 100 + d1.d * 10 + d0.d AS n
    FROM (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d0,
         (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d1,
         (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d2
) n ON n.n < c.qty;

DROP TEMPORARY TABLE seed_catalog;

SELECT COUNT(*) AS total_units, SUM(status = 'AVAILABLE') AS available FROM equipment_units;
