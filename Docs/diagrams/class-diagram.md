# Class Diagram

The nine implemented classes of Phase 3 — six models in `com.labbooking.model` and three DAOs in `com.labbooking.dao` — with their real attributes and methods. All model fields are `private final`; all models are immutable and expose getters only.

```mermaid
classDiagram
    direction LR

    class User {
        -int id
        -String fullName
        -String email
        -String role
        +User(int, String, String, String)
        +getId() int
        +getFullName() String
        +getEmail() String
        +getRole() String
        +isAdmin() boolean
    }

    class Material {
        -int id
        -String subject
        -String name
        -String variant
        -int availableCount
        +Material(int, String, String, String, int)
        +getId() int
        +getSubject() String
        +getName() String
        +getVariant() String
        +getAvailableCount() int
        +label(String name, String variant)$ String
        +toString() String
    }

    class EquipmentUnit {
        -int id
        -int materialId
        -String materialName
        -String serialNo
        -String status
        +EquipmentUnit(int, int, String, String, String)
        +getId() int
        +getMaterialId() int
        +getMaterialName() String
        +getSerialNo() String
        +getStatus() String
    }

    class BookingRequest {
        -int id
        -String requestCode
        -int studentId
        -String studentName
        -String purpose
        -LocalDate returnDate
        -String status
        -LocalDateTime createdAt
        +BookingRequest(int, String, int, String, String, LocalDate, String, LocalDateTime)
        +getId() int
        +getRequestCode() String
        +getStudentId() int
        +getStudentName() String
        +getPurpose() String
        +getReturnDate() LocalDate
        +getStatus() String
        +getCreatedAt() LocalDateTime
    }

    class BookingItem {
        -int id
        -int requestId
        -int materialId
        -String materialName
        -int quantity
        -String status
        +BookingItem(int, int, int, String, int, String)
        +getId() int
        +getRequestId() int
        +getMaterialId() int
        +getMaterialName() String
        +getQuantity() int
        +getStatus() String
    }

    class Penalty {
        -int id
        -int requestId
        -String reason
        -BigDecimal amount
        -boolean paid
        +Penalty(int, int, String, BigDecimal, boolean)
        +getId() int
        +getRequestId() int
        +getReason() String
        +getAmount() BigDecimal
        +isPaid() boolean
    }

    class UserDao {
        +register(String fullName, String email, String password) User
        +authenticate(String email, String password) User
        +hash(String password)$ String
        +verify(String password, String stored)$ boolean
        -sha256Hex(String text)$ String
    }

    class InventoryDao {
        +listSubjects() List~String~
        +listMaterials(String subject) List~Material~
        +listUnits() List~EquipmentUnit~
        +addUnit(int materialId, String serialNo) void
        +setUnitStatus(int unitId, String status) void
        ~readUnit(ResultSet rs)$ EquipmentUnit
    }

    class BookingDao {
        +addToCart(List~BookingItem~ cart, Material material, int quantity)$ void
        +submit(int studentId, String purpose, LocalDate returnDate, List~BookingItem~ cart) String
        +listByStudent(int studentId) List~BookingRequest~
        +listByStatus(String status) List~BookingRequest~
        +listItems(int requestId) List~BookingItem~
        +listUnitsForRequest(int requestId) List~EquipmentUnit~
        +decideItem(int itemId, boolean approve) void
        +decideRequest(int requestId, boolean approve) void
        +recordIssue(int requestId) void
        +recordReturn(int requestId, Set~Integer~ damagedUnitIds) void
        -freeUnits(Connection c, int requestId, Integer materialId)$ void
        -syncRequestStatus(Connection c, int requestId)$ void
        -update(Connection c, String sql, int requestId)$ void
        -listRequests(String where, Object param) List~BookingRequest~
    }

    User "1" --> "0..*" BookingRequest : studentId
    BookingRequest "1" --> "1..*" BookingItem : requestId
    Material "1" --> "0..*" BookingItem : materialId
    Material "1" --> "0..*" EquipmentUnit : materialId
    BookingRequest "0..1" --> "0..*" EquipmentUnit : request_id
    BookingRequest "1" --> "0..*" Penalty : requestId

    UserDao ..> User : returns
    InventoryDao ..> Material : returns
    InventoryDao ..> EquipmentUnit : returns
    BookingDao ..> BookingRequest : returns
    BookingDao ..> BookingItem : returns and merges
    BookingDao ..> EquipmentUnit : returns
    BookingDao ..> Material : reads for the cart
```

**Derived attributes** — four fields are *not* columns of any table. They are filled by a `JOIN` or an aggregate in the DAO query so a table row needs no extra query, and they are therefore derived attributes of the Java classes only:

| Class | Attribute | Derived from |
|---|---|---|
| `Material` | `availableCount` | `COUNT(equipment_units WHERE status = 'AVAILABLE')` |
| `EquipmentUnit` | `materialName` | `JOIN materials` |
| `BookingItem` | `materialName` | `JOIN materials` |
| `BookingRequest` | `studentName` | `JOIN users`, for the administrator's queue |

**Notes**

- `$` marks a static member, `~` package-private, `-` private, `+` public (Mermaid notation).
- The associations above are navigable **by foreign-key value, not by object reference**: no model holds a collection or a pointer to another model. `BookingRequest` has no `List<BookingItem>` field; the DAO fetches item lines with `listItems(requestId)`. This keeps the models flat, exactly as the tables are.
- Statuses are plain `String`s matching the database `ENUM`s, so there are no status enum classes.
- `Penalty` is implemented but **not yet used** by any DAO or screen (penalties are Planned).
- Not shown, because they are infrastructure rather than domain classes: `Main` (JavaFX `Application` plus the static session), `Database` (JDBC connection helper) and the four `ui` views `LoginView`, `RegisterView`, `StudentView`, `AdminView`, which call the three DAOs.

**Cross-check (Phase 8 requirement):** every attribute, method name, parameter list and return type above was read out of the `.java` files in `src/main/java/com/labbooking/model/` and `/dao/`. `throws SQLException` is omitted from the DAO signatures for readability; every DAO method except `addToCart`, `hash`, `verify` and `sha256Hex` declares it.
