# Activity Diagram

The booking lifecycle as the code actually runs it: `BookingDao.submit` → `decideItem` / `decideRequest` → `recordIssue` → `recordReturn`.
Both failure branches required by the README are shown: **insufficient stock rolls the whole submission back**, and **a rejection frees the units it was holding**.

```mermaid
flowchart TD
    START((Start)) --> LOGIN["Student logs in"]
    LOGIN --> BROWSE["Pick subject, read available counts"]
    BROWSE --> ADD["Add material and quantity to cart"]
    ADD --> MORE{"Another material?"}
    MORE -- yes --> BROWSE
    MORE -- no --> FORM["Enter purpose and return date"]
    FORM --> SUBMIT["Submit"]

    SUBMIT --> VALID{"Purpose filled and return date in the future?"}
    VALID -- no --> MSG1["Show the DAO message, cart kept"]
    MSG1 --> FORM

    VALID -- yes --> TX["Begin transaction"]
    TX --> INSERT["Insert booking_requests PENDING and one booking_items line per material"]
    INSERT --> STOCK{"Enough AVAILABLE units for every line?"}
    STOCK -- no --> ROLLBACK["Roll back the whole transaction - nothing saved"]
    ROLLBACK --> MSG2["Tell the student which material was short, cart kept"]
    MSG2 --> BROWSE

    STOCK -- yes --> BOOK["Mark that many units BOOKED and stamp them with the request id"]
    BOOK --> COMMIT["Commit, return the request code"]
    COMMIT --> QUEUE["Request appears in the admin PENDING queue"]

    QUEUE --> REVIEW["Administrator reviews the item lines"]
    REVIEW --> DECIDE{"Decide per item or for the whole batch"}
    DECIDE -- reject --> FREE["Set the line REJECTED and release its BOOKED units back to AVAILABLE"]
    DECIDE -- approve --> OK["Set the line APPROVED, units stay BOOKED"]
    FREE --> SYNC
    OK --> SYNC

    SYNC["Recompute the request status from its lines"] --> PENDINGLEFT{"Any line still PENDING?"}
    PENDINGLEFT -- yes --> REVIEW
    PENDINGLEFT -- no --> ANYOK{"Any line approved?"}
    ANYOK -- no --> REJ["Request REJECTED"]
    REJ --> ENDR((End))
    ANYOK -- yes --> APPR["Request APPROVED"]

    APPR --> ISSUE["Administrator records the issue"]
    ISSUE --> ISSUED["Units ISSUED, request ISSUED, student collects the equipment"]
    ISSUED --> BACK["Student returns the equipment"]
    BACK --> INSPECT{"Any unit damaged?"}
    INSPECT -- yes --> DMG["Selected units DAMAGED, the rest AVAILABLE"]
    INSPECT -- no --> AVL["All units of the request back to AVAILABLE"]
    DMG --> RET
    AVL --> RET
    RET["Request RETURNED, units unlinked from it"] --> PEN["Penalty for a late or damaged return - PLANNED, not implemented"]
    PEN --> ENDOK((End))
```

**Notes tying the diagram to the code**

- The status recomputation box is `BookingDao.syncRequestStatus`: while any line is `PENDING` the header is left alone, otherwise it becomes `APPROVED` if any line was approved, else `REJECTED`. A request can therefore never be approved with every line rejected.
- `ISSUED` and `RETURNED` are written outright by `recordIssue` / `recordReturn`, not computed — which is why the admin buttons guard on the current status.
- A damaged unit is later put back into circulation by the separate *Mark repaired* action, not by this flow.
