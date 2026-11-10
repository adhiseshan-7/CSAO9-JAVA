# Programming in Java – Assignment 1 (CO1)
**Campus Smart-Parking System**
Name: S. Adhiseshan | Register No: 1911260132 | Java 21
GitHub link: `https://github.com/<your-username>/CSA09-1911260132-Assignment1` *(fill in after pushing)*

---

## Task 1 – Problem Analysis and Objects (4)

**Inputs:** vehicle type, registration number, user category (student / staff / visitor), pass validity, entry time, exit time, kWh used (EV).
**Outputs:** allocated {floor, slot}, ticket (unique number, entry time), released slot, parking fee, "facility full" / "invalid" messages.

**Candidate classes and why each is needed**

| Class | Responsibility | Why it is needed |
|---|---|---|
| `Vehicle` (abstract) + `TwoWheeler`, `Car`, `ElectricCar` | Hold regNo; supply hourly rate | Fee depends on type; subclasses remove type-testing if-else chains |
| `ParkingSlot` / `ParkingFacility` | Hold slot occupancy per floor, allocate, release | Rule "a slot never holds two vehicles" lives in one place |
| `ParkingTicket` | Ticket no., vehicle, floor, slot, entry time | Links a vehicle to its slot; ticket no. generated from a static counter |
| `User` (`Student`, `Staff`, `Visitor`) | Pass status | Decides free vs. paid |
| `FeeCalculator` / `PricingRule` | Compute fee | Pricing in one place |
| `Chargeable` (interface) | `chargingFee(kWh)` | Only EVs charge |

**Business rules:** a slot holds at most one vehicle; ticket numbers are never reused; a vehicle with an active ticket cannot enter twice (no duplicate regNo); release of an invalid or free slot fails; every started hour is billed; valid pass holders (staff/students) pay 0, visitors pay full.
**Assumptions:** floors are numbered from 0; slots are allocated first-free; time is in whole minutes; rates are Two-wheeler Rs 10/h, Car Rs 30/h, EV parking Rs 30/h + Rs 12/kWh charging; input via `Scanner`.

**Alternative – parallel arrays `regNo[]`, `type[]`, `entryTime[]`:**
Search by regNo is O(n) in both designs (an object array also scans), but a `HashMap<String, Vehicle>` makes it O(1) average; with parallel arrays a new attribute needs a new array and edits wherever indexes are kept in sync (error-prone). Space: object ≈ 16 B header + one reference per field; parallel arrays ≈ one reference/primitive per field per vehicle with no header, so slightly less memory but much weaker structure. Both O(n) space.

## Task 2 – Data Representation (3)

| Field | Type | Reason |
|---|---|---|
| slot number, floor | `int` | Small whole indices (array subscripts) |
| vehicle number | `String` | Alphanumeric (e.g. TN02B2) |
| entry time | `long` (epoch ms) or `LocalDateTime` | Avoids overflow of `int` |
| duration | `int` minutes | Whole minutes |
| fee | `double` (or `BigDecimal` for real money) | Fractional rates |
| slots | jagged `String[][]` | Floors may differ in size |

**Input:** `Scanner` (interactive), arguments for tests.
**Casting risk:** `double fee = (minutes / 60) * rate;` — integer division gives 130/60 = 2, so 2×30 = Rs 60 instead of the correct 90 (3 started hours). Resolution: ceiling with integer arithmetic `(minutes + 59) / 60` (used in `Vehicle.calculateFee`), then multiply by the rate. A wrong cast `(int) (rate * hours)` would also truncate fractional rates.

**Alternative – 1-D array `index = floor*slotsPerFloor + slot`, or `ArrayList<String[]>`:**
The 1-D formula assumes equal floor sizes; with unequal floors an offsets (prefix-sum) array is required. Locate slot: O(1) for jagged array and 1-D array; `ArrayList<String[]>` get is also O(1). Space: all O(F·S); jagged adds F row headers; ArrayList adds a small overhead and is resizable.

## Task 3 – Control Logic (4)

**Allocation pseudocode**
```
FUNCTION allocate(vehicleNo)
    FOR floor FROM 0 TO floors − 1 DO
        FOR slot FROM 0 TO slotsOnFloor(floor) − 1 DO
            IF slots[floor][slot] IS EMPTY THEN
                slots[floor][slot] ← vehicleNo
                RETURN (floor, slot)
            END IF
        END FOR
    END FOR
    RETURN NULL
END FUNCTION
```
Search stops at the first free slot (early `return`); if both loops end, the facility is full.

**Fee pseudocode (slabs)**
```
FUNCTION fee(minutes, rate, hasPass)
    IF hasPass THEN RETURN 0
    hours ← (minutes + 59) DIV 60
    IF hours = 0 THEN RETURN 0
    ELSE IF hours = 1 THEN amount ← rate
    ELSE amount ← rate + (hours − 1) × rate × 0.8      // example slab
    END IF
    RETURN MIN(amount, dailyMax)
END FUNCTION
```
In Java the slab uses `if–else if`; `switch` is used on vehicle category / pass type.

**Alternative – per-floor free counter + stack of free slots:** allocate becomes: find a floor with `freeCount > 0` (O(F)), pop a slot (O(1)) → **O(F)**, release pushes back **O(1)**. Original scan: allocate worst case **O(F·S)**, release **O(1)**. Extra space: scan approach O(1); counters + stacks O(F + F·S) = O(F·S).

## Task 4 – Classes, Objects and Constructors (4)
`ParkingTicket` has overloaded constructors chained with `this(...)`:
```java
ParkingTicket(Vehicle v, int floor, int slot) { this(v, floor, slot, System.currentTimeMillis()); }
ParkingTicket(Vehicle v, int floor, int slot, long entryMs) {
    this.ticketNo = ++counter; this.vehicle = v; ...
}
```
The one full constructor guarantees every object is valid (no half-built tickets); `this` refers to the current object and chaining avoids duplicated logic.
**Alternative:** default constructor + setters allows an invalid intermediate object; static factory methods (`ParkingTicket.issue(...)`) give names and validation. Creating n objects: **O(n) time, O(n) space** in all three; setters add n×k extra calls (constant factor).

## Task 5 – static Members and Object Life Cycle (3)
- `static int ticketCounter` — one value shared by all tickets; an instance variable would restart at 0 for each ticket and reuse numbers. Also `static int totalOccupancy`.
- `static` methods/block: utility such as `FeeCalculator.ceilHours(int)`, static block to load rates.
- After exit the ticket is no longer referenced (removed from active-ticket structure) → eligible for garbage collection; a copy kept in history stays alive.
- Objects are passed by reference value: the callee can modify the same object but cannot re-point the caller's variable; methods return the object reference (e.g. `allocate` returns a new `int[]`).
**Alternative:** instance counter (wrong) or an external `IdGenerator` object – O(1) either way, but instance counters break uniqueness.

## Task 6 – Inheritance Hierarchy (4)
```
        «abstract» Vehicle ──────────┐
         - regNo : String            │ implements
         # ratePerHour() : double    │
         + calculateFee(int)         ▼
          ▲        ▲          «interface» Chargeable
          │        │           + chargingFee(int kWh)
     TwoWheeler   Car ◄─────────── ElectricCar (extends Car, implements Chargeable)
```
Hierarchical (TwoWheeler, Car from Vehicle) and multilevel (ElectricCar → Car → Vehicle). Subclasses inherit `regNo`, `calculateFee`, `equals`; each adds its rate; `ElectricCar` adds charging. `super(regNo)` invokes the parent constructor.
**Constructor trace for `new ElectricCar("TN03C3")`:** `ElectricCar(..)` → calls `super` → `Car(..)` → calls `super` → `Vehicle(..)` sets regNo; then Car body, then ElectricCar body (Vehicle → Car → ElectricCar).
Users: `Student`, `Staff`, `Visitor` extend `User` (hierarchical).
**Alternative – composition (Vehicle has-a VehicleType holding the rate):** one Vehicle class, new type = new data (no new class); loses compile-time type distinction and type-specific behaviour (e.g. charging). Time O(1), space +1 reference per vehicle.

## Task 7 – Overriding, final, Object methods (4)
- `ratePerHour()` is overridden in each subclass because the rate differs; `chargingFee` is implemented by `ElectricCar`.
- `@Override` makes the compiler check the signature; rules: same name and parameters, compatible return type, access not narrower (protected → protected/public).
- `final`: `regNo` (immutable identity), a class such as `ParkingTicket` (cannot be subclassed), `calculateFee` if the algorithm must not change.
- `equals`/`hashCode` use `regNo`, so two Vehicle objects with the same registration number are equal and hash together; `toString` returns type + regNo.
**Alternative – single `calculateFee()` with `switch(type)`:** O(k) worst case (O(1) with a jump table), and adding a type means editing that method (1 existing class changed) versus 0 with overriding.

## Task 8 – Polymorphism (4)
- **Compile-time:** overloaded `calculateFee(int minutes)` / `calculateFee(int h, int m)`.
- **Runtime:** `Vehicle v = new ElectricCar(..); v.calculateFee(130)` — declared type `Vehicle`, actual type `ElectricCar`, the JVM executes `Car.calculateFee` (inherited) which calls the `ElectricCar/Car` `ratePerHour()`. No `if (type == ...)` chain is needed.
**Last line of FeeDemo prints `true`** because `equals` compares only `regNo` and uses `instanceof Vehicle`, so `Car("TN02B2")` equals `ElectricCar("TN02B2")`. For this system that is correct: a registration number identifies one physical vehicle, so they can't both be parked.
**Alternative – `instanceof` + casts:** O(k) checks per call vs. O(1) dispatch; more code and every new type edits the chain.

## Task 9 – Abstract classes and Interfaces (4)
`Vehicle` is abstract (shared state `regNo`, constructor, common `calculateFee`, abstract `ratePerHour`). `Chargeable` is an interface because charging is a capability that unrelated classes (e.g. `ElectricBike`) can add, and a class may implement several interfaces. Default/static interface methods can offer shared helpers (e.g. a default `chargingSummary`).
**Alternative:** Vehicle as interface can't hold `regNo` state or a constructor; Chargeable as abstract class blocks multiple inheritance (ElectricCar couldn't extend Car). Per-call time and per-object space are essentially identical, O(1).

## Task 10 – Packages and Encapsulation (3)
Packages: `parking.model` (Vehicle…), `parking.service` (ParkingFacility, FeeCalculator), `parking.app` (Main). Fields `private`, getters public, constructors `protected`/public as needed, validating setters (e.g. reject empty regNo). Wrapper classes: `Integer` in collections; `==` on `Integer` values above 127 compares references — use `equals`.
**Alternative – public fields:** validation repeated at every call site (m places) vs once in the setter; each check is O(1) per update, but one missed check corrupts state.

## Task 11 – Implementation, Testing, Evaluation (5)
Programs: `Task03_ParkingFacility/ParkingFacility.java`, `Task09_FeeDemo/FeeDemo.java` — output matches the expected output exactly (see `output.txt`).

| # | Case | Input | Expected | Actual |
|---|---|---|---|---|
| 1 | Normal | 3 vehicles on {2,1} floors | [0,0] [0,1] [1,0] | ✔ |
| 2 | Boundary – last slot / full | 4th vehicle | null | ✔ |
| 3 | Release valid | release(0,1) | true | ✔ |
| 4 | Invalid – already free | release(0,1) again | false | ✔ |
| 5 | Invalid – bad floor | release(5,0) | false | ✔ |
| 6 | Reuse of freed slot | allocate | [0,1] | ✔ |
| 7 | Two-wheeler 130 min | 3 h × 10 | Rs 30.0 | ✔ |
| 8 | Car 130 min | 3 h × 30 | Rs 90.0 | ✔ |
| 9 | EV 130 min + 10 kWh | 90 + 120 | Rs 210.0 | ✔ |
| 10 | Equal regNo, different class | Car vs ElectricCar | true | ✔ |
| 11 | Zero minutes | `(0+59)/60 = 0` | Rs 0 | ✔ by formula |
| 12 | Slab boundary 60 / 61 min | 1 h / 2 h | 1×rate / 2×rate | ✔ by formula |

**Alternative – ArrayList / HashMap for active tickets (measured worst-case lookup, `Task11_Benchmark`):**

| n | array | ArrayList | HashMap |
|---|---|---|---|
| 20 | 659 ns | 462 ns | 167 ns |
| 500 | 3 594 ns | 1 438 ns | 47 ns |
| 5 000 | 25 698 ns | 11 417 ns | 14 ns |
| 50 000 | 322 237 ns | 276 873 ns | 19 ns |

Array and ArrayList lookup grow linearly, **O(n)**; HashMap stays flat, **O(1)** average. Space: array/ArrayList O(n); HashMap O(n) with a larger constant (entry objects and table). Evaluation: the design is correct for the given rules; for 5×100 slots the O(F·S) allocation scan is acceptable, but the exit lookup should use a HashMap keyed by regNo (which also detects duplicate regNo in O(1)).

## Task 12 – Re-evaluate and Adapt (8)

**A. Impact Analysis (2).** New: `EBike` (flat fee), `DeliveryVan` (2× car rate 9 AM–5 PM), visitor one-day pass, 5 floors with different sizes. Jagged `String[][]` already supports unequal floors (a 1-D formula would not), so slot storage is unaffected; constructor just receives `{20,40,60,80,100}`. Only `ParkingFacility` callers pass new arrays. Pricing depends on time of day, which `calculateFee(int minutes)` doesn't know → signature must change/extend. No existing class needs modification apart from the fee method.

**B. Evaluation (2).** New vehicle types are added only by writing new subclasses (open/closed principle ✔). But pricing is spread across subclasses (rate in each) while pass/visitor logic is elsewhere — so time-based and pass rules would be duplicated. Existing tested classes (`Vehicle`, `Car`, `TwoWheeler`, `ParkingFacility`) can remain unchanged if the new rule is added externally.

**C. Decision – MODIFY (2).** Retain the hierarchy and facility; add a strategy:
```java
interface PricingRule { double fee(Vehicle v, LocalDateTime in, LocalDateTime out, User u); }
class FlatFeeRule, TimeBandedRule (van: 2×car 9–17), PassRule (day pass, monthly pass)
```
- `EBike extends Vehicle` (rate unused; `FlatFeeRule`), `DeliveryVan extends Vehicle` (`TimeBandedRule`).
- `DayPass` on `Visitor`; the pricing rules sit in one package, easy to combine.
- Facility: `new ParkingFacility(new int[]{20,40,60,80,100})` – no code change.
- Ease of future types: one subclass + (if needed) one rule class; zero edits to existing tests.
- Tests: e-bike 5 h = flat Rs X; van 10:00–12:00 = 2×car; van 18:00–19:00 = car rate; visitor with day pass = paid once; 5-floor facility fills in order.
- Trade-offs: more classes/indirection, but each rule is isolated and testable. A full redesign wasn't needed because the hierarchy, polymorphism and jagged arrays already absorbed the change.
