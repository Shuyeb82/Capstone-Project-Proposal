# Data Flow Diagram

ICT Lab Resource Booking System. Drawn from the implemented code (`src/main/java/com/labbooking/`) and `db/schema.sql`.

Notation: rectangles are external entities, rounded boxes are processes, cylinders are data stores.
Penalties are **Planned, not implemented** — `penalties` exists in the schema and `Penalty.java` exists, but no code reads or writes it (see the proposal, §11).

## Context level (Level 0)

```mermaid
flowchart LR
    STU["Student"]
    ADM["Administrator"]
    SYS("0. ICT Lab Resource Booking System")

    STU -- "registration / login credentials" --> SYS
    STU -- "booking request: purpose, return date, item lines" --> SYS
    SYS -- "catalogue with available counts" --> STU
    SYS -- "request status, validation and error messages" --> STU

    ADM -- "login credentials" --> SYS
    ADM -- "approve / reject decision, issue, return, new unit, repair" --> SYS
    SYS -- "pending / approved / issued requests, units held, inventory" --> ADM
```

## Level 1

```mermaid
flowchart TB
    STU["Student"]
    ADM["Administrator"]

    P1("1.0 Authenticate and register")
    P2("2.0 Browse catalogue")
    P3("3.0 Submit booking request")
    P4("4.0 Review and decide")
    P5("5.0 Record issue and return")
    P6("6.0 Manage inventory")
    P7("7.0 Charge penalties - PLANNED")

    D1[("D1 users")]
    D2[("D2 materials")]
    D3[("D3 equipment_units")]
    D4[("D4 booking_requests")]
    D5[("D5 booking_items")]
    D6[("D6 penalties - PLANNED")]

    STU -- "name, email, password" --> P1
    ADM -- "email, password" --> P1
    P1 -- "new student row, password hash" --> D1
    D1 -- "stored hash, role" --> P1
    P1 -- "session user and role" --> STU
    P1 -- "session user and role" --> ADM

    STU -- "chosen subject" --> P2
    D2 -- "subject, name, variant" --> P2
    D3 -- "count of AVAILABLE units" --> P2
    P2 -- "material list with available count" --> STU

    STU -- "cart lines, purpose, return date" --> P3
    D3 -- "current AVAILABLE stock" --> P3
    P3 -- "request header, PENDING" --> D4
    P3 -- "one line per material" --> D5
    P3 -- "mark quantity units BOOKED" --> D3
    P3 -- "request code, or rollback message" --> STU
    D4 -- "own requests and status" --> P3
    D5 -- "own item lines" --> P3
    P3 -- "My Bookings list" --> STU

    ADM -- "item or batch decision" --> P4
    D4 -- "requests by status" --> P4
    D5 -- "item lines" --> P4
    P4 -- "item status, computed request status" --> D4
    P4 -- "APPROVED or REJECTED per line" --> D5
    P4 -- "free units of rejected lines" --> D3
    P4 -- "updated queue" --> ADM

    ADM -- "record issue, record return with damaged units" --> P5
    D3 -- "units held by the request" --> P5
    P5 -- "ISSUED, then AVAILABLE or DAMAGED" --> D3
    P5 -- "request ISSUED, then RETURNED" --> D4
    P5 -- "confirmation" --> ADM

    ADM -- "material and serial number, repair a damaged unit" --> P6
    P6 -- "new unit, status change" --> D3
    D2 -- "material list" --> P6
    D3 -- "all units with status" --> P6
    P6 -- "inventory table, duplicate-serial error" --> ADM

    D4 -. "late or damaged return" .-> P7
    P7 -. "reason, amount, paid flag" .-> D6
    D6 -. "penalty list" .-> STU
    P7 -. "mark paid" .-> ADM
```
