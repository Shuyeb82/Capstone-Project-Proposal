# Use Case Diagram

Actors and use cases of the implemented prototype (Phases 4–6). Everything inside the system boundary is working code except the two dashed penalty cases, which are **Planned**.

```mermaid
flowchart LR
    STU(["Student"])
    ADM(["Administrator"])

    subgraph SYS["ICT Lab Resource Booking System"]
        direction TB
        UC1("Register a student account")
        UC2("Log in")
        UC3("Log out")
        UC4("Browse materials by subject")
        UC5("View all lab stock")
        UC6("Add material to request cart")
        UC7("Remove cart line")
        UC8("Submit booking request")
        UC9("View my bookings and status")

        UC10("View requests by status")
        UC11("Approve or reject one item")
        UC12("Approve or reject whole request")
        UC13("Record issue")
        UC14("Record return and mark damaged units")
        UC15("Add equipment unit")
        UC16("Mark damaged unit repaired")

        UC17("View my penalties - PLANNED")
        UC18("Create penalty and mark paid - PLANNED")
    end

    STU --- UC1
    STU --- UC2
    STU --- UC3
    STU --- UC4
    STU --- UC5
    STU --- UC6
    STU --- UC7
    STU --- UC8
    STU --- UC9
    STU -.- UC17

    ADM --- UC2
    ADM --- UC3
    ADM --- UC10
    ADM --- UC11
    ADM --- UC12
    ADM --- UC13
    ADM --- UC14
    ADM --- UC15
    ADM --- UC16
    ADM -.- UC18

    UC4 -. "extend" .-> UC6
    UC6 -. "include" .-> UC8
    UC11 -. "include" .-> UC10
    UC13 -. "include" .-> UC10
    UC14 -. "include" .-> UC10
```

**Relationships worth reading off the diagram**

- *Log in* is shared: `LoginView` routes to `StudentView` or `AdminView` by `User.role`.
- *Submit booking request* includes the cart: `BookingDao.submit` refuses an empty cart, a blank purpose, or a return date that is not in the future.
- *Record issue* and *Record return* are guarded by the request's current status, so they can only follow an approval.
