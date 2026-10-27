**ICT Lab Resource Booking System**

The ICT Lab Resource Booking System is a desktop application for managing laboratory equipment. Students can request equipment, and a lab administrator can approve requests, issue items, and record returns.

The project focuses on borrowing laboratory equipment, such as breadboards and sensors. It does not manage laboratory room or class-time reservations.

**Project Objective**

The main objective is to make equipment borrowing easier to manage. The system records available equipment, student requests, issued items, returns, and penalties.

**Project Proposal**

[Capstone Proposal](https://github.com/Shahariar6172/lab-Booking/blob/main/docs/proposal/capstone-proposal.md)

**Users**

The system has two types of users:

- **Student:** Browses equipment, submits requests, and checks booking status and penalties.
- **Lab Administrator:** Manages equipment, reviews requests, records issues and returns, and manages penalties.

**Student Workflow**

1. Register an account and log in.
2. Select a subject or laboratory activity.
3. Choose a material, its variant, and the required quantity.
4. Add the material to the request list.
5. Review the list and remove unwanted items.
6. Enter the purpose and a future return date.
7. Submit the request.
8. Check its status in **My Bookings**.

Adding the same material variant again increases its quantity in the list. Items submitted together share a request code.

Submitting a request does not mean the equipment has been issued. The administrator must first review the request and then record the equipment handover.

**Equipment Availability**

Students can see how many units are available. Materials with no available units are shown as **0 available** and cannot be added to a request.

Refreshing stock keeps the current request list. Changing the subject clears the list. Students can use **Show all lab stock** to browse equipment beyond the selected subject.

If there is not enough stock when a request is submitted, the request is not partially saved. The student can adjust the list and try again.

**Administrator Workflow**

The administrator can:

- View pending requests.
- Approve or reject individual items or a complete request batch.
- Record when approved equipment is issued.
- Record returned equipment and whether it is damaged.
- Add equipment with a unique serial number for each physical unit.
- Mark repaired equipment as available.
- View penalties and mark them as paid.

Approval and issuing are separate steps: approval accepts the request, while issuing records that the student has received the equipment.

**Inventory Management**

Each physical unit has its own record and serial number. For example, three breadboards are registered as three separate units.

The system tracks whether each unit is available, booked, issued, or damaged. Equipment becomes available again after an appropriate return or repair update.

**Material Catalog**

Materials are organized by subject, material name, and variant to help students find what they need.

A catalog entry describes a material; it does not add physical equipment. The administrator must register actual units before students can borrow them.

**Booking Records and Penalties**

Students can view their booking status in **My Bookings**. A booking can be pending, approved, rejected, issued, or returned.

The system also records penalties for late returns or damaged equipment. Students can view their penalties, and the administrator can update the payment status.

**Technology Used**

The application uses Java and JavaFX for its desktop interface and MySQL or MariaDB to store records.

**Project Scope**

This first version covers the basic equipment borrowing process: selecting materials, submitting requests, reviewing requests, issuing equipment, and recording returns and penalties.

**Setup and Run**

Prerequisites: JDK 21 or newer, Maven 3.9+, and MySQL or MariaDB.

Copy the example configuration and fill in your own database credentials. The real `db.properties` is not committed.

```
copy src\main\resources\db.properties.example src\main\resources\db.properties
```

Build the project:

```
mvn clean compile
```

Run the application:

```
mvn javafx:run
```

Run the tests:

```
mvn test
```

**Database setup:** create the database and load the demo data:

```
mysql -u root -p < db/schema.sql
mysql -u root -p lab_booking < db/seed.sql
```

**Demo accounts**:

| Name | Email | Role |
|---|---|---|
| Lab Administrator | `admin@lab.edu` | ADMIN |
| Al-Shahariar | `shahariar@lab.edu` | STUDENT |

`admin@mbstu.ac.bd` (ADMIN).