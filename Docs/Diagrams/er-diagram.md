# Entity Relationship Diagram

The six tables of `db/schema.sql`, field for field. `PK` primary key, `FK` foreign key, `UK` unique constraint.

```mermaid
erDiagram
    users {
        int id PK "AUTO_INCREMENT"
        varchar_100 full_name "NOT NULL"
        varchar_190 email UK "NOT NULL"
        varchar_100 password_hash "NOT NULL, saltHex$sha256Hex"
        enum role "STUDENT or ADMIN, default STUDENT"
    }

    materials {
        int id PK "AUTO_INCREMENT"
        varchar_80 subject "NOT NULL"
        varchar_80 name "NOT NULL"
        varchar_80 variant "NOT NULL, default empty string"
        unique subject_name_variant UK "UNIQUE subject, name, variant"
    }

    booking_requests {
        int id PK "AUTO_INCREMENT"
        varchar_20 request_code "NOT NULL, INDEX, shared by one submission"
        int student_id FK "NOT NULL, users.id, ON DELETE CASCADE"
        varchar_255 purpose "NOT NULL"
        date return_date "NOT NULL"
        enum status "PENDING APPROVED REJECTED ISSUED RETURNED, default PENDING"
        datetime created_at "NOT NULL, default CURRENT_TIMESTAMP"
    }

    equipment_units {
        int id PK "AUTO_INCREMENT"
        int material_id FK "NOT NULL, materials.id, ON DELETE CASCADE"
        varchar_50 serial_no UK "NOT NULL"
        enum status "AVAILABLE BOOKED ISSUED DAMAGED, default AVAILABLE"
        int request_id FK "NULL, booking_requests.id, ON DELETE SET NULL"
    }

    booking_items {
        int id PK "AUTO_INCREMENT"
        int request_id FK "NOT NULL, booking_requests.id, ON DELETE CASCADE"
        int material_id FK "NOT NULL, materials.id, ON DELETE CASCADE"
        int quantity "NOT NULL, CHECK quantity greater than 0"
        enum status "PENDING APPROVED REJECTED ISSUED RETURNED, default PENDING"
        unique request_material UK "UNIQUE request_id, material_id"
    }

    penalties {
        int id PK "AUTO_INCREMENT"
        int request_id FK "NOT NULL, booking_requests.id, ON DELETE CASCADE"
        varchar_255 reason "NOT NULL"
        decimal amount "NOT NULL, DECIMAL 8 2"
        boolean paid "NOT NULL, default FALSE"
    }

    users            ||--o{ booking_requests : "places"
    booking_requests ||--o{ booking_items    : "contains"
    materials        ||--o{ booking_items    : "is requested as"
    materials        ||--o{ equipment_units  : "has physical"
    booking_requests |o--o{ equipment_units  : "currently holds"
    booking_requests ||--o{ penalties        : "incurs - PLANNED"
```

**Reading the cardinalities**

- A student places zero or more requests; every request belongs to exactly one student.
- A request contains one or more item lines, at most one line per material (`UNIQUE(request_id, material_id)`) — adding the same material twice raises the quantity instead.
- A material has zero or more physical units; `equipment_units.request_id` is nullable, so a unit belongs to at most one request at a time and is unlinked again on return.
- `penalties` is created by the schema but not used by any code yet (**Planned**).

**Cross-check (Phase 8 requirement):** every column, type, default, `UNIQUE`, `CHECK`, `INDEX` and `ON DELETE` rule above was read out of `db/schema.sql`. Mermaid attribute types cannot contain brackets, so `varchar(100)` is written `varchar_100` and the `ENUM` values live in the comment column.
