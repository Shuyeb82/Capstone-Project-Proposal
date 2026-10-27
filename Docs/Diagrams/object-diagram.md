# Object Diagram

One concrete snapshot of the running system, consistent with [`class-diagram.md`](class-diagram.md) and with the demo data in `db/seed.sql`: the student **Al-Shahariar ** has submitted one request with **two item lines**, the administrator has approved it, and **three units** are held by it.

Mermaid has no object-diagram type, so each box below is one *instance*: the box name is the object name and the `<<Class>>` line is its class, in place of the UML `object : Class` underline.

```mermaid
classDiagram
    direction LR

    class shahariar {
        <<User>>
        id = 2
        fullName = Al-Shahariar 
        email = shahariar@lab.edu
        role = STUDENT
    }

    class req7 {
        <<BookingRequest>>
        id = 7
        requestCode = REQ-261024143015
        studentId = 2
        studentName = Al-Shahariar 
        purpose = EEE Lab experiment 4
        returnDate = 2026-10-31
        status = APPROVED
        createdAt = 2026-10-24 14.30
    }

    class line1 {
        <<BookingItem>>
        id = 11
        requestId = 7
        materialId = 64
        materialName = Breadboard
        quantity = 2
        status = APPROVED
    }

    class line2 {
        <<BookingItem>>
        id = 12
        requestId = 7
        materialId = 66
        materialName = Sensor
        quantity = 1
        status = APPROVED
    }

    class breadboard {
        <<Material>>
        id = 64
        subject = EEE Lab
        name = Breadboard
        variant = 830 points
        availableCount = 2
    }

    class dht11 {
        <<Material>>
        id = 66
        subject = EEE Lab
        name = Sensor
        variant = DHT11
        availableCount = 2
    }

    class bb001 {
        <<EquipmentUnit>>
        id = 1
        materialId = 64
        materialName = Breadboard
        serialNo = EEE-BB-001
        status = BOOKED
    }

    class bb002 {
        <<EquipmentUnit>>
        id = 2
        materialId = 64
        materialName = Breadboard
        serialNo = EEE-BB-002
        status = BOOKED
    }

    class dht001 {
        <<EquipmentUnit>>
        id = 9
        materialId = 66
        materialName = Sensor
        serialNo = EEE-DHT-001
        status = BOOKED
    }

    shahariar --> req7 : studentId = 2
    req7 --> line1 : requestId = 7
    req7 --> line2 : requestId = 7
    breadboard --> line1 : materialId = 64
    dht11 --> line2 : materialId = 66
    breadboard --> bb001 : materialId = 64
    breadboard --> bb002 : materialId = 64
    dht11 --> dht001 : materialId = 66
    req7 --> bb001 : request_id = 7
    req7 --> bb002 : request_id = 7
    req7 --> dht001 : request_id = 7
```

**Why this snapshot is internally consistent**

- Two item lines, quantities 2 and 1 → exactly three units are held, which is what `BookingDao.submit` books.
- `breadboard.availableCount = 2`: the seed gives EEE Lab four breadboards; `EEE-BB-001` and `EEE-BB-002` are `BOOKED` by this request, leaving `EEE-BB-003` and `EEE-BB-004` available.
- `dht11.availableCount = 2`: of the seed's three `EEE-DHT-*` units, only `EEE-DHT-001` is held here.
- Both lines are `APPROVED` and no line is `PENDING`, so `syncRequestStatus` sets the request to `APPROVED` — the next legal action is *Record issue*, which turns all three units and the request to `ISSUED`.
- The request is listed at most once per material (`UNIQUE(request_id, material_id)`), so a second breadboard request would have raised `quantity` instead of adding a third line.

Material and unit ids follow the seed's insertion order: the 63 `IOT Lab` rows are inserted first, so EEE Lab's Breadboard is material 64 and its Sensor (DHT11) is material 66. The unit ids above are illustrative of that same ordering.

**One honest mismatch to note:** the last three links are database state, not Java fields. `equipment_units.request_id` is a column, but the `EquipmentUnit` class has no `requestId` attribute — `BookingDao.listUnitsForRequest(7)` is what resolves that link at runtime. The link is drawn because the snapshot would be unreadable without it.
