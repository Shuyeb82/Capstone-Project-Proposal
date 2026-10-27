# ICT Lab Resource Booking System — Final Capstone Project Proposal (Part I)

- **Course:** ICT 3107 / ICT 3108 — Part I: Final Capstone Project Proposal and Prototyping
- **Student:** Al-Shahariar 
- **ID:** IT-24057
- **Repository:** <https://github.com/Shahariar6172/lab-Booking>
- **Date of this version:** 2026-10-24
- **Status of the work described here:** working prototype complete for the student and administrator borrowing workflow; penalties specified but not implemented.

---

## 1. Title

ICT Lab Resource Booking System — a desktop application for requesting, approving, issuing and returning ICT laboratory equipment.

## 2. Abstract

ICT laboratories lend physical components — breadboards, logic ICs, jumper wires, microcontroller boards, sensors — to students for coursework and projects. In most departments this is tracked on paper or in a spreadsheet, so nobody can answer "how many 830-point breadboards are free right now?" without walking to the store room. This project delivers a Java 21 / JavaFX desktop application backed by MySQL that models each physical unit as its own record with a serial number, lets students assemble a multi-item request and submit it in a single transaction, and lets a laboratory administrator approve or reject items, record the handover, and record the return of each unit as available or damaged. Stock counts are derived from unit status, so availability cannot drift from reality. The prototype implements nine classes (six models, three data access objects) and four screens; a full manual QA script of 33 steps accompanies it. Penalty handling is designed and present in the schema but deliberately left unimplemented in Part I.

## 3. Introduction

An ICT laboratory is an inventory problem wearing a timetable's clothes. The scarce resource is not the room but the components inside it, and those components are consumed, lost, and broken. A student who needs three breadboards for an EEE Lab experiment currently has no way to know whether three are free before the session starts, and an administrator who has lent out twelve units has no record of which unit went to whom.

This proposal describes a system that keeps the authoritative record of equipment in one database and gives both parties a screen onto it. It deliberately excludes laboratory room and class-time reservation, which is a scheduling problem with different constraints and is normally already handled by the departmental timetable.

## 4. Problem Statement

Equipment lending in the ICT laboratory is recorded manually, which produces five concrete failures:

1. **Availability is unknown.** Students request equipment that is already lent out; administrators discover the shortage at the counter.
2. **No per-unit accountability.** "Three breadboards" is recorded as a quantity, not as three identified units, so a damaged or missing unit cannot be traced to the request that held it.
3. **Approval and handover are conflated.** A request written in a register looks identical whether or not the student has physically received the items.
4. **Partial fulfilment corrupts the record.** When only two of three requested units are free, a paper process quietly issues two and the request no longer matches what was asked.
5. **No history.** There is no queryable record of who borrowed what, when it was due, and whether it came back damaged, so late returns and damage cannot be charged consistently.

## 5. Motivation

The problem is small enough to be solved completely by one student in one development cycle, and real enough that the result is usable rather than demonstrative. It also exercises the parts of software engineering the course assesses: a normalised relational schema with enforced referential integrity, a transactional operation whose correctness matters (stock must never oversell), a trust boundary that must be validated and must not store plaintext passwords, and a layered architecture that keeps SQL out of the user interface. A to-do list application would exercise none of these.

## 6. Aim and Objectives

**Aim:** to replace the manual ICT laboratory equipment lending record with a single-source-of-truth desktop system that tracks every physical unit through its full borrowing lifecycle.

**Objectives:**

| # | Objective | Status |
|---|---|---|
| O1 | Design a normalised schema separating the material catalogue from physical units | Implemented |
| O2 | Authenticate students and administrators with hashed passwords and role-based routing | Implemented |
| O3 | Let a student browse live availability and assemble a multi-item request | Implemented |
| O4 | Submit a request atomically, rejecting the whole request if stock is short | Implemented |
| O5 | Let an administrator decide items individually or as a batch, and derive request status from the item decisions | Implemented |
| O6 | Record issue and return separately, per unit, including damage | Implemented |
| O7 | Maintain inventory: add units with unique serial numbers, mark repaired units available | Implemented |
| O8 | Charge and track penalties for late or damaged returns | Planned |
| O9 | Produce the six required design diagrams from the finished code | Implemented |

## 7. Scope and Limitations

**In scope:** student registration and login; subject-based catalogue browsing with live availability; multi-item request assembly and atomic submission; the administrator's approve / reject / issue / return workflow; per-unit inventory management including serial numbers, damage and repair; booking history for both roles.

**Out of scope, and why:**

- **Laboratory room and class-time booking** — excluded by the project definition in `README.md`; it is a timetable problem, not an inventory problem.
- **Penalties** — schema and model class exist; no user interface and no data access code. Listed as Planned throughout this document.
- **Email or push notification, reporting dashboards, PDF export, a role management screen** — none of these change the correctness of the borrowing record, so they are deferred.
- **Multi-laboratory / multi-campus** — a single laboratory is assumed. There is no `lab_id` anywhere in the schema.

**Known limitations of the prototype:** it is a two-tier desktop application with a direct database connection, so it assumes a trusted laboratory network; there is no connection pool, so it suits laboratory-counter concurrency (a few simultaneous users), not campus-wide load; the session is a static field, so one application instance serves one logged-in user at a time; passwords use salted SHA-256 rather than a memory-hard function such as bcrypt or Argon2.

## 8. Proposed Solution

A JavaFX desktop client talking directly to a MySQL database over JDBC, in three layers:

- **Catalogue vs. units.** `materials` describes *what kinds of things exist* (subject, name, variant). `equipment_units` is one row per *physical object*, with a unique serial number and a status of `AVAILABLE`, `BOOKED`, `ISSUED` or `DAMAGED`. Availability is therefore never stored as a number; it is `COUNT(units WHERE status = 'AVAILABLE')`, and cannot disagree with the units themselves.
- **Request header vs. request lines.** `booking_requests` is one submission (purpose, return date, status, shared request code); `booking_items` is one material-and-quantity line inside it. This is what lets an administrator approve one line and reject another.
- **Reservation at submission time.** Submitting a request does not merely record an intent: inside one transaction it re-checks stock, inserts the header and all lines, and flips exactly that many units to `BOOKED`, stamping them with the request id. If any material is short, the entire transaction rolls back and the student is told which material and by how much. There is no partial save.
- **Separate approval and handover.** Approval (`APPROVED`) accepts the request; recording the issue (`ISSUED`) states that the student physically has the items; recording the return releases each unit back to `AVAILABLE` or marks it `DAMAGED`.

## 9. Stakeholders

| Stakeholder | Interest | How the system serves it |
|---|---|---|
| Student | Knowing what is free; a request that is either fully accepted or clearly refused | Live availability, atomic submission, My Bookings status |
| Laboratory administrator / technician | Knowing where every unit is; deciding requests quickly | Status-filtered request lists, per-item decisions, per-unit return |
| Course teacher | Knowing that equipment for an experiment is available for a session | Indirect in Part I: the administrator can see pending demand per subject |
| Department / asset owner | Accountability for capital equipment | Serial-numbered units, damage records, request history |
| Developer (the author) | A maintainable codebase that can be extended | Three-layer separation; no SQL in the user interface |

## 10. Literature and Existing-System Comparison

| Existing approach | What it does well | Why it does not fit |
|---|---|---|
| Paper register / spreadsheet | Zero cost, no training | No live availability, no per-unit identity, no enforced validation; two people editing it conflict silently |
| University room / timetable booking systems (e.g. a campus LMS module) | Mature calendar handling | Model a time slot on a resource, not a pool of consumable identical units; cannot express "three of these twelve" |
| Library circulation systems (e.g. Koha) | Per-copy ("item") identity, loan and return, fines — genuinely close in model | Built around bibliographic records and cataloguing standards; heavy to deploy and adapt for a departmental laboratory's components |
| General IT asset managers (e.g. Snipe-IT, GLPI) | Serial-numbered assets, check-in/check-out | Administrator-facing; no student self-service request workflow, no subject-based catalogue, and licensing/hosting overhead for a single laboratory |

The design adopts the library model's most important idea — the *item* (a physical copy) distinct from the *record* (what kind of thing it is) — and keeps the student-facing request-and-approval flow that asset managers lack. The engineering practices follow standard references on requirements, process models and layered design (Sommerville; Pressman) and on quality attributes (ISO/IEC 25010).

## 11. Functional Requirements

| ID | Requirement | Status | Where |
|---|---|---|---|
| FR-01 | A visitor can register a student account (name, unique email, password ≥ 8 characters) and is logged in immediately | Implemented | `RegisterView`, `UserDao.register` |
| FR-02 | A user logs in with email and password and is routed to the student or administrator screen by role | Implemented | `LoginView`, `Main.enter` |
| FR-03 | A failed login reports only "Invalid email or password", without revealing whether the email exists | Implemented | `LoginView`, `UserDao.authenticate` |
| FR-04 | A student can filter the catalogue by subject and see each material's available count | Implemented | `StudentView`, `InventoryDao.listMaterials` |
| FR-05 | A material with zero available units shows `0 available` and cannot be added to a request | Implemented | `StudentView` |
| FR-06 | Adding a material already in the request list increases its quantity instead of creating a second line | Implemented | `BookingDao.addToCart` |
| FR-07 | A student can remove a line from the request list | Implemented | `StudentView` |
| FR-08 | Refreshing stock keeps the request list; changing subject clears it; "Show all lab stock" ignores the subject filter and keeps the list | Implemented | `StudentView.loadStock` |
| FR-09 | Submission requires a non-empty purpose, a return date strictly in the future, and at least one line | Implemented | `BookingDao.submit` |
| FR-10 | Submission is atomic: stock is re-checked, header and all lines are inserted, and that many units are marked `BOOKED`; if any material is short the whole transaction rolls back and the student is told which material and how many are available | Implemented | `BookingDao.submit` |
| FR-11 | Everything submitted together shares one request code | Implemented | `BookingDao.submit` |
| FR-12 | A student can see their requests with status, and the item lines of a selected request | Implemented | `StudentView`, `BookingDao.listByStudent` / `listItems` |
| FR-13 | An administrator can list requests by status (`PENDING`, `APPROVED`, `ISSUED`) and see the selected request's items and the units it holds | Implemented | `AdminView`, `BookingDao.listByStatus` / `listUnitsForRequest` |
| FR-14 | An administrator can approve or reject a single item, or the whole request batch | Implemented | `BookingDao.decideItem` / `decideRequest` |
| FR-15 | Request status at the decision stage is computed from its item lines, never set by hand; while any line is pending the header is unchanged | Implemented | `BookingDao.syncRequestStatus` |
| FR-16 | Rejecting an item or a batch releases the units that item was holding | Implemented | `BookingDao.freeUnits` |
| FR-17 | An administrator can record the issue of an approved request: units and request become `ISSUED` | Implemented | `BookingDao.recordIssue` |
| FR-18 | An administrator can record a return, marking selected units `DAMAGED` and all other held units `AVAILABLE`; the request becomes `RETURNED` | Implemented | `BookingDao.recordReturn` |
| FR-19 | An administrator can add a physical unit to a material with a serial number, and a duplicate serial number is rejected with a readable message | Implemented | `InventoryDao.addUnit` |
| FR-20 | An administrator can mark a `DAMAGED` unit `AVAILABLE` again (repaired) | Implemented | `InventoryDao.setUnitStatus` |
| FR-21 | A penalty is created automatically on a late or damaged return; the student can view their penalties; the administrator can mark one paid | **Planned** | `penalties` table and `Penalty` model exist; no data access or user interface |

## 12. Non-Functional Requirements

| ID | Attribute | Requirement | Status |
|---|---|---|---|
| NFR-01 | Security | Passwords are never stored in plaintext. Format: `saltHex$sha256Hex(saltHex + password)`, 16-byte random salt per user | Implemented |
| NFR-02 | Security | Every SQL statement is a `PreparedStatement` with bound parameters; no string concatenation of user input | Implemented |
| NFR-03 | Security | Database credentials live in `src/main/resources/db.properties`, which is git-ignored; only `db.properties.example` with placeholders is committed | Implemented |
| NFR-04 | Reliability | No operation may leave the database half-written: submission, item decision, issue and return each run in one transaction with rollback | Implemented |
| NFR-05 | Correctness | Stock can never oversell: availability is derived from unit status and re-checked inside the submission transaction | Implemented |
| NFR-06 | Usability | Every refusal states its reason in words (not only colour), sourced from one place so the rule and the message cannot diverge | Implemented |
| NFR-07 | Accessibility | Form fields use real `Label`s, not prompt text, so the label survives typing and is announced by screen readers; colour is a second cue only | Implemented |
| NFR-08 | Performance | Availability counts and display names are fetched by join/aggregate in the list query, not one query per table row; `equipment_units` is indexed on `(material_id, status)` | Implemented |
| NFR-09 | Maintainability | Three layers: `ui` → `dao` → JDBC. No SQL in a view, no JavaFX type in a data access object | Implemented |
| NFR-10 | Portability | Java 21 and JDBC only; runs against MySQL 8 or MariaDB; `utf8mb4` throughout | Implemented |
| NFR-11 | Testability | Validation and the cart merge rule live in testable classes, not in view event handlers; password hashing is `public static` and tested without a database | Implemented |

## 13. Feasibility

**Technical:** the stack is Java 21, JavaFX 21, MySQL and JDBC — all free, all installed and verified working locally (MySQL 8.4.3). No component of the design requires a technology the author has not already used in the prototype. The prototype itself is the strongest feasibility evidence: 1,638 lines across 16 Java files, building with `mvn -q clean test` green.

**Economic:** zero licence cost. JDK, Maven, JavaFX, MySQL/MariaDB, Git and the IDE are free; development hardware is the author's own laptop; deployment is to an existing laboratory computer.

**Operational:** the administrator's workflow mirrors what the laboratory already does on paper (review, hand over, take back), so adoption needs no process change beyond entering the existing stock once, which the inventory screen supports. Two roles and four screens is within reach of a short handover briefing.

**Schedule:** Part I's scope — prototype, diagrams, proposal — is already largely delivered (see §23); the remaining work is diagrams, three unit tests, screenshots and validation.

**Legal / ethical:** the system stores a name, an email address and a password hash. No other personal data, no payment data. Passwords are hashed and credentials are not committed to the repository.

## 14. System Architecture

A two-tier (client–database) architecture with three logical layers inside the client:

```
+-------------------------------------------------------------+
|  Presentation — src/main/java/com/labbooking/ui             |
|  LoginView   RegisterView   StudentView   AdminView         |
|  (JavaFX VBox/TabPane; builds controls, shows DAO messages)  |
+----------------------------+--------------------------------+
                             | model objects, plain Java types
+----------------------------v--------------------------------+
|  Data access — com.labbooking.dao                           |
|  UserDao   InventoryDao   BookingDao                        |
|  (PreparedStatement, transactions, all validation rules)    |
+----------------------------+--------------------------------+
                             | java.sql.Connection
+----------------------------v--------------------------------+
|  com.labbooking.Database — reads db.properties, hands out   |
|  a Connection (one method, no pool)                         |
+----------------------------+--------------------------------+
                             | JDBC (mysql-connector-j 8.4.0)
+----------------------------v--------------------------------+
|  MySQL 8 / MariaDB — lab_booking (6 tables)                 |
|  constraints: UNIQUE, FOREIGN KEY, ENUM, CHECK              |
+-------------------------------------------------------------+
```

Two architectural decisions are worth stating explicitly because they are deliberate and unusual:

1. **Validation lives in the data access layer, not the user interface.** `UserDao.register` checks the name, email and password; `BookingDao.submit` checks the purpose, the return date and stock. A view's only job is to show `exception.getMessage()`. The rule is therefore written once and holds for every caller, including tests, instead of being duplicated per screen.
2. **Request status at the decision stage is computed, not assigned.** `syncRequestStatus` derives the header status from the item lines, which makes the state "all items rejected but request approved" unrepresentable.

## 15. Technology Stack and Justification

| Choice | Version | Why this, and not the alternative |
|---|---|---|
| Java | 21 (LTS) | Course language; long-term support release; `maven.compiler.release=21` so a newer local JDK still produces Java 21 bytecode |
| JavaFX | 21.0.5 | The supported desktop UI toolkit for modern Java. Swing is effectively frozen; a web stack would add a server, a browser and a protocol to a single-laboratory problem |
| MySQL / MariaDB | 8.4 / any 10.x | Relational, because the data is relational: the whole design rests on foreign keys and uniqueness constraints that a document store would make the application enforce |
| JDBC + `mysql-connector-j` | 8.4.0 | Plain SQL with `PreparedStatement`. An ORM (Hibernate/JPA) would hide the transaction and the `UPDATE ... LIMIT n` that reserves units — the two places where correctness actually lives — behind generated SQL, and would add a mapping layer to nine small classes |
| Maven | 3.9+ | Declarative dependencies and `mvn javafx:run` through `javafx-maven-plugin`; a grader can build with one command |
| JUnit | 5.11.3 | Unit tests for the logic a grader can break, without a database |
| **Not used:** Spring, Hibernate, Lombok, a connection pool, an FXML loader per screen | — | Each would add configuration and a dependency without changing what the application does. Views are plain `VBox` subclasses; FXML would mean a second file plus a loader per screen for the same screen |

## 16. Methodology — Software Development Process Model

**Chosen model: Incremental development.** The system was built as a sequence of increments, each of which compiles, runs and adds one usable slice of behaviour: project skeleton → database schema and seed data → model and data access classes → login and registration → the complete student request workflow → the complete administrator workflow. Every increment ended with a working application and a commit, and every increment's deviations from its own plan were recorded in `todo.md` before the next one started.

Incremental suits this project for three reasons. The requirements were known and stable from the start — they are written out in `README.md` and did not change during development — so the exploratory iteration of an agile process had nothing to discover. The project is a solo effort against a fixed academic deadline, so the value of a process is in ordering the work and keeping something demonstrable at all times, not in coordinating people. And the features decompose naturally into dependent slices: the administrator's approval screen is meaningless before a student can submit a request, which is meaningless before the catalogue and the schema exist. That dependency order *is* the increment order.

The alternatives were rejected on specific grounds, not on fashion. **Waterfall** would have required the six design diagrams and the full proposal to be finalised before coding — and this project's experience shows why that fails: the schema gained a `request_id` column on `equipment_units` only once Phase 5 and 6 were designed in detail, because without it nothing links a held unit to the request holding it. A Waterfall entity-relationship diagram drawn in week one would have been wrong, and the proposal would have described software nobody had written. Building first and drawing the diagrams from the finished code (Phase 8) guarantees the diagrams match reality. **Scrum** was rejected because most of its machinery is social: sprint planning, daily stand-ups, a review and a retrospective with a product owner and a development team presuppose more than one person and a customer whose priorities shift. With a single developer, a frozen requirement set and no stakeholder available for sprint review, Scrum's ceremonies would cost time and produce no information. What was worth keeping from agile practice was kept without the framework: small increments, a working build at every step, and a written record of each increment's decisions.

## 17. System Analysis and Design

The analysis fixes four modelling decisions, each of which is forced by a requirement rather than chosen for elegance:

1. **A material is not a unit.** "Breadboard (830 points)" in EEE Lab is a catalogue entry; its four physical breadboards are four rows with serial numbers `EEE-BB-001…EEE-BB-004`. Required by per-unit accountability (FR-18, FR-19) and by availability being derived rather than stored (NFR-05).
2. **A request is a header plus lines.** Per-item approval (FR-14) is impossible if a request is a single row.
3. **A unit knows which request holds it.** `equipment_units.request_id` is nullable: non-null while `BOOKED` or `ISSUED`, null again after return. Without it, "which units does this request hold?" has no answer, and FR-17/FR-18 are unimplementable.
4. **Status values are strings matching the database `ENUM`s, not Java enum classes.** This avoids four near-empty classes; an invalid value is rejected by MySQL rather than silently stored.

The design maps these onto six tables (§19) and nine Java classes (§18, class diagram). The behaviour of the system is the lifecycle `PENDING → APPROVED | REJECTED → ISSUED → RETURNED`, with two branches that matter: insufficient stock at submission (nothing is saved) and rejection at decision time (held units are released).

## 18. System Diagrams

All six required diagrams are produced **from the finished code** and live in `docs/diagrams/` as Mermaid, so they render directly on GitHub. All six are written; what remains in Phase 8 (§23) is the field-for-field cross-check against `db/schema.sql` and the `.java` files. Each file is linked below.

| Diagram | File | What it shows |
|---|---|---|
| Data Flow Diagram | [`../diagrams/data-flow-diagram.md`](../diagrams/data-flow-diagram.md) | Context level and Level 1: the Student and Administrator external entities, the request / decision / issue / return processes, and the catalogue, request and penalty data stores |
| Use Case Diagram | [`../diagrams/use-case-diagram.md`](../diagrams/use-case-diagram.md) | Actors Student and Administrator against the use cases of FR-01 … FR-20, with penalties marked as planned |
| Activity Diagram | [`../diagrams/activity-diagram.md`](../diagrams/activity-diagram.md) | The submit → approve → issue → return lifecycle, including the insufficient-stock rollback branch and the rejection branch that frees units |
| Entity Relationship Diagram | [`../diagrams/er-diagram.md`](../diagrams/er-diagram.md) | The six tables of `db/schema.sql` with primary keys, foreign keys and cardinalities, field for field |
| Class Diagram | [`../diagrams/class-diagram.md`](../diagrams/class-diagram.md) | The nine implemented classes with real attributes and methods, including the four derived display fields (`Material.availableCount`, `EquipmentUnit.materialName`, `BookingItem.materialName`, `BookingRequest.studentName`) marked as derived |
| Object Diagram | [`../diagrams/object-diagram.md`](../diagrams/object-diagram.md) | One consistent snapshot: the student Al-Shahariar , one approved request with two item lines, and the three units it holds |

The prototype satisfies the assignment's "at least five classes represented in the class diagram" requirement with nine: `User`, `Material`, `EquipmentUnit`, `BookingRequest`, `BookingItem`, `Penalty`, `UserDao`, `InventoryDao`, `BookingDao`. Eight of the nine are called by user-interface code; `Penalty` is not, because penalties are Planned.

## 19. Database Design

Database `lab_booking`, `utf8mb4` / `utf8mb4_unicode_ci`, defined in `db/schema.sql` with demo data in `db/seed.sql`.

| Table | Key columns | Constraints that carry design intent |
|---|---|---|
| `users` | `id` PK, `full_name`, `email`, `password_hash`, `role` | `email` UNIQUE (duplicate registration refused by the database, not only by code); `role ENUM('STUDENT','ADMIN')` |
| `materials` | `id` PK, `subject`, `name`, `variant` | `UNIQUE(subject, name, variant)`; `variant` defaults to `''` rather than NULL, because MySQL does not enforce uniqueness across NULLs |
| `equipment_units` | `id` PK, `material_id` FK, `serial_no`, `status`, `request_id` FK (nullable) | `serial_no` UNIQUE; `status ENUM('AVAILABLE','BOOKED','ISSUED','DAMAGED')`; `request_id` is `ON DELETE SET NULL`; `INDEX(material_id, status)` for the availability count |
| `booking_requests` | `id` PK, `request_code`, `student_id` FK, `purpose`, `return_date`, `status`, `created_at` | `status ENUM('PENDING','APPROVED','REJECTED','ISSUED','RETURNED')`; `ON DELETE CASCADE` from `users`; `INDEX(request_code)` |
| `booking_items` | `id` PK, `request_id` FK, `material_id` FK, `quantity`, `status` | `UNIQUE(request_id, material_id)` — the database-level half of the cart merge rule; `CHECK(quantity > 0)` |
| `penalties` | `id` PK, `request_id` FK, `reason`, `amount`, `paid` | `amount DECIMAL(8,2)`, never a floating-point type, because it is money. Table exists; unused in Part I |

**Normalisation:** all six tables are in third normal form. The catalogue/unit split removes the repeating group ("three breadboards" as a quantity on one row); the header/line split removes the repeating group of materials inside a request; no non-key column depends on another non-key column. The four *derived* attributes carried by the Java model classes (`availableCount`, `materialName`, `studentName`) are deliberately **not** columns — they are query results, so they cannot go stale.

**Seed data for demonstration:** two administrators, two students, 76 materials across four subjects (IOT Lab, EEE Lab, IoT Project, Robotics Lab) and 908 units, all seeded `AVAILABLE`. Units are not written out one row at a time: the seed holds a quantity per material in a temporary `seed_catalog` table and generates the unit rows by joining it against a 0–999 number table, which is why 908 units fit in a 90-line file. No unit is pre-set `DAMAGED`, so the zero-availability and repair paths are reached by using the application (issue a return with a damaged unit, then repair it) rather than from the seed. Demo account credentials belong in `README.md`'s setup section (Phase 10) and are deliberately not reproduced in this document.

## 20. User Interface and Prototype Design

Four screens, all plain JavaFX built in code (no FXML), switched by `Main.show(Parent)` setting the root of a single `Scene`:

| Screen | Class | Contents |
|---|---|---|
| Login | `ui/LoginView` | Email, password, Login, link to Register. Failure shows "Invalid email or password" |
| Register | `ui/RegisterView` | Name, email, password; creates a `STUDENT` and logs in immediately |
| Student | `ui/StudentView` | `TabPane`: **Browse & request** (subject dropdown → material table with available count, quantity spinner, request list with remove, purpose, return date picker, Submit) and **My bookings** (request table → item lines) |
| Administrator | `ui/AdminView` | `TabPane`: **Requests** (status dropdown `PENDING`/`APPROVED`/`ISSUED` → request table → items table + held-units table, with Approve/Reject item, Approve/Reject request, Record issue, Record return) and **Inventory** (unit list, add unit with serial, mark repaired) |

Interaction decisions: quantity is a `Spinner` (1–99), so no numeric parsing or range checking exists to get wrong; damaged units on return are chosen by multi-selecting rows rather than a checkbox column; after any administrator action the screen reloads and re-selects the same request by id, so deciding items one by one does not lose the selection. Messages are red for failure and green for success, but the wording alone identifies the outcome — colour is never the only cue.

Screenshots for this section are captured in Phase 9 into `docs/screenshots/`.

## 21. Implementation Strategy

The order of implementation follows the increment order of §16, and each step was completed and committed before the next began: project skeleton and build (`0792387`) → schema and seed data (`7862e4f`) → six model classes and three data access objects (`c337ab3`) → login and registration (`c9b7da5`) → the student workflow (`bc198a8`) → the administrator workflow and the validation/message pass (`5d9249d`, `a60b162`).

Three conventions held throughout, and they are the reason the code is 1,638 lines rather than several thousand:

- **Write a rule once.** Every validation rule lives in exactly one data access method; views print the message. A rule is never re-checked in a view.
- **Reuse before adding.** The student request list is a `List<BookingItem>` with ids left at 0 rather than a new cart class; `AdminView` static-imports `StudentView.col(...)` for table columns instead of copying it or inventing a helper class; the administrator's three status lists are one query (`listByStatus`) rather than three.
- **Deviations are recorded, not hidden.** Every phase in `todo.md` ends with a "deviations from the list above" note explaining what was built differently and why. That record is the source of §17's design decisions and of this document's Implemented/Planned marks.

## 22. Testing and Quality Assurance Strategy

Three layers, deliberately small:

1. **Automated unit tests (JUnit 5)** for the logic a grader can actually break without a database. Currently green: three password-hashing tests (`src/test/java/com/labbooking/dao/UserDaoPasswordTest.java`), one of which asserts that the hashing code validates the real hash committed in `db/seed.sql`. Planned in Phase 6b: the cart quantity-merge rule, the insufficient-stock full-rollback path, and return-date validation.
2. **Live-database verification of every data access method.** Each method of the three data access objects was executed against the local MySQL instance — login, duplicate email, stock counts, submission, short-stock rollback, item and batch decisions, issue, return with a damaged unit, duplicate serial, repair — with 40 assertions passing. The throwaway script was not committed; the schema and seed were re-run afterwards to restore clean demo data.
3. **Manual user-interface QA script:** [`../qa-steps.md`](../qa-steps.md) — 33 numbered steps with the expected result for each, covering registration and login failures, the student happy path, every validation refusal, the full administrator workflow, and a cross-check that the student sees the administrator's decisions. It is written to be run after a schema/seed reset, since the steps mutate data. This document does not repeat those steps; the script is the single source.

Not done, and stated as such rather than claimed: the user interface has not been click-tested end to end (that is Phase 9), and there is no automated integration or UI test. `mvn -q clean verify` is a Phase 9 gate.

## 23. Schedule, Milestones and Deliverables

Part I is due **Wednesday 28 October 2026 at 11:59**, five calendar days after development started. Phases 1–7 were all completed on **2026-10-24**; the remaining four phases are scheduled one per day against that deadline, with the diagrams — the largest remaining deliverable and the one with the most marks attached — given two days.

| Phase | Deliverable | Milestone | Date | Status |
|---|---|---|---|---|
| 1 | `pom.xml`, package layout, `.gitignore`, `db.properties.example` | M1: project builds (`mvn -q clean compile`) | Sat 24 Oct | **Done** |
| 2 | `db/schema.sql`, `db/seed.sql` | M2: schema and demo data load clean on a fresh database | Sat 24 Oct | **Done** |
| 3 | 6 model classes, 3 data access objects, password hashing + tests | M3: ≥ 5 classes implemented; `mvn -q clean test` green | Sat 24 Oct | **Done** |
| 4 | `LoginView`, `RegisterView`, session | M4: a user can register, log in and be routed by role | Sat 24 Oct | **Done** |
| 5 | `StudentView` | M5: a student can browse, build a request and submit it atomically | Sat 24 Oct | **Done** |
| 6 | `AdminView`, validation/message pass | M6: the full approve → issue → return workflow runs | Sat 24 Oct | **Done** |
| 7 | `docs/proposal/capstone-proposal.md` (this document) | M7: proposal with process model, schedule and Gantt complete | Sat 24 Oct | **Done** |
| 6b | 3 JUnit tests: cart merge, stock rollback, return date | M8: the three breakable rules are covered by tests | Sun 25 Oct | Planned |
| 8 | Six diagrams in `docs/diagrams/` | M9: all six required diagrams exist, render on GitHub, and match the code and schema | Sun 25 – Mon 26 Oct | Drafted; cross-check pending |
| 9 | `docs/screenshots/`, `mvn -q clean verify`, LaTeX via Pandoc | M10: demo walked end to end; proposal exported | Tue 27 Oct | Planned |
| 10 | `README.md` setup section, `docs/assignment-compliance.md`, push | M11: repository pushed and the URL submitted | Wed 28 Oct, before 11:59 | Planned |

The submission itself is a website URL, so the repository must be public and complete at that moment; two attempts are allowed, which leaves one correction after the first submission.

**Deliverables at submission:** the proposal (this file, plus a Pandoc-generated `.tex`), six diagram files, the working prototype source, `db/schema.sql` and `db/seed.sql`, the QA script, screenshots, the compliance table, and the repository URL (§30).

## 24. Gantt Chart

```mermaid
gantt
    title ICT Lab Resource Booking System — Part I schedule (24–28 October 2026; due Wed 28 Oct, 11:59)
    dateFormat YYYY-MM-DD
    axisFormat %d %b
    tickInterval 1day
    todayMarker off

    section Completed (2026-10-24)
    Phase 1 Project skeleton            :done, p1, 2026-10-24, 1d
    Phase 2 Database schema and seed    :done, p2, 2026-10-24, 1d
    Phase 3 Models and DAOs (9 classes) :done, p3, 2026-10-24, 1d
    Phase 4 Login and registration UI   :done, p4, 2026-10-24, 1d
    Phase 5 Student workflow            :done, p5, 2026-10-24, 1d
    Phase 6 Admin workflow + validation :done, p6, 2026-10-24, 1d
    Phase 7 Proposal and planning       :done, p7, 2026-10-24, 1d
    M1-M7 prototype and proposal        :milestone, m7, 2026-10-24, 0d

    section Planned (25–28 October 2026)
    Phase 6b Three JUnit tests          :active, p6b, 2026-10-25, 1d
    Phase 8 Six diagrams from the code  :active, p8, 2026-10-25, 2d
    M8-M9 tests and diagrams            :milestone, m9, 2026-10-26, 0d
    Phase 9 Validate, screenshots, LaTeX :p9, 2026-10-27, 1d
    Phase 10 README, compliance, push   :p10, 2026-10-28, 1d
    M11 URL submitted (due 11:59)       :milestone, m11, 2026-10-28, 0d
```

The first bars carry the real date on which that work was done: all of Phases 1–7 were completed on 2026-10-24, which is why they stack on a single day rather than spreading across the chart. The remaining four bars are planned against the published deadline of Wednesday 28 October 2026. Phase 6b and Phase 8 run in parallel on 25 October because the three unit tests touch only `BookingDao` while the diagrams are a documentation task; nothing after that overlaps, since Phase 9 validates what Phase 8 produced.

## 25. Risks and Mitigation

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Diagrams drawn from intent rather than code, and contradicting the implementation | Medium | High (directly graded) | Diagrams are produced *after* the code, in Phase 8, and Phase 8 ends with a field-for-field cross-check of the ER diagram against `db/schema.sql` and the class diagram against the `.java` files |
| R2 | Concurrent submissions overselling the same unit | Low (one laboratory counter) | High (correctness) | Stock is re-checked and units are reserved inside one transaction; the reservation is an `UPDATE` of a bounded number of `AVAILABLE` rows, so a loser rolls back rather than overselling |
| R3 | Database credentials committed to a public repository | Low | High | `db.properties` is git-ignored; only a placeholder example is committed; Phase 9 greps the repository for credential patterns |
| R4 | Pandoc or a LaTeX engine unavailable, so no `.tex`/PDF is produced | Medium | Low | The LaTeX template is *recommended*, not required, and the submission is a website URL — so the Markdown proposal rendered on GitHub is the artefact that is actually graded. If the toolchain is missing, the submission notes say so instead of claiming a compiled PDF |
| R9 | The repository is private, or Phase 10 is unfinished, at 11:59 on 28 October | Low | **Critical** (zero marks) | Submission is a URL, not an upload, so the repository state at the deadline *is* the submission: Phase 10 ends with pushing and opening the public URL in a signed-out browser. Two attempts are allowed, leaving room for one correction |
| R5 | Salted SHA-256 is weaker than a memory-hard hash under offline attack | Medium | Medium | Acceptable for a prototype on a laboratory network, and stated as a limitation (§7); the hash format is isolated in `UserDao.hash`/`verify`, so moving to bcrypt or Argon2 is a change in one class |
| R6 | Penalties expected by a reader of `README.md` but not implemented | High | Low | Marked **Planned** consistently in §6, §7, §11, §18 and the QA script, so no reader can mistake the state |
| R7 | Live demonstration fails because the data was mutated by earlier testing | Medium | Medium | `db/schema.sql` + `db/seed.sql` reset to a known state in seconds; the QA script's step 0 is that reset and all expected counts depend on it |
| R8 | Scope creep into room booking or notifications, at the cost of Part I deliverables | Low | Medium | Exclusions are written down in `README.md` and §7, with a "deliberately not doing" list in `todo.md` |

## 26. Expected Outcomes

On completion of Part I: a laboratory administrator can enter the real stock once and then run the whole lending cycle from the application, with every physical unit traceable to the request holding it; a student can see true availability before asking, and receives either a complete request or a clear refusal naming the material that is short. For assessment specifically: a proposal covering the required components, all six required diagrams generated from the real code, and a prototype of nine classes and four screens that performs the full workflow described in `README.md` — with the one excluded feature, penalties, labelled Planned rather than quietly omitted.

## 27. Future Improvements

In priority order, each a small change because of where the current code puts the relevant rule:

1. **Penalties (FR-21).** Add a `PenaltyDao`, auto-create on a late or damaged return inside `recordReturn`'s existing transaction, a student penalty list and an administrator "mark paid". The table and the model class already exist.
2. **Stronger password hashing.** Swap bcrypt or Argon2 into `UserDao.hash`/`verify`, with verification falling back to the old format for existing rows.
3. **Overdue visibility.** The data is already there (`return_date` plus `status = 'ISSUED'`): one query gives the administrator an overdue list, which is the prerequisite for automatic penalties.
4. **Per-unit history.** An append-only `unit_events` table would turn the current "where is this unit now?" into "where has this unit been?" without changing any existing table.
5. **Reservation by date range.** Requesting equipment *for next Tuesday* rather than from now requires availability to become time-dependent — the one genuinely large change on this list, since availability stops being a single `COUNT`.
6. **Operational hardening if the deployment grows:** a connection pool, per-user sessions, and an export or reporting screen.

## 28. Conclusion

The ICT Lab Resource Booking System addresses a narrow, real problem — the ICT laboratory does not know what equipment it has free, or who is holding the rest — and solves it by making the physical unit the unit of record and the transaction the unit of change. Part I delivers more than a mock-up: the schema, nine classes, four screens and the complete student and administrator workflow are implemented and verified against a live database, and the one feature not built is labelled as such everywhere it appears. The incremental process model was chosen because the requirements were stable and the features stack in dependency order, and it is also why the diagrams are drawn last: from software that exists rather than software that was planned. What remains for Part I is bounded.
## 30. Repository

**GitHub:** <https://github.com/Shahariar6172/lab-Booking>



