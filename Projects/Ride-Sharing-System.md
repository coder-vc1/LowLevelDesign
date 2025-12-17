---
# Low-Level Design (LLD) for a Ride-Sharing System (Uber/Rapido style) in Java
---

### 1. Functional & Non-functional Requirements

**Functional:**

* **User Management:** Register Riders and Drivers.
* **Driver Availability:** Drivers can toggle availability and have a current location.
* **Ride Booking:** Rider inputs source/destination; System matches a driver.
* **Ride Lifecycle:** Start Ride \rightarrow End Ride \rightarrow Calculate Fare.
* **Pricing:** Support dynamic pricing logic.

**Non-Functional:**

* **Concurrency:** Handle multiple riders booking simultaneously.
* **Extensibility:** Pluggable pricing strategies and matching algorithms.
* **Modularity:** Separation of concerns (MVC).

---

### 2. Rough Flow of Program

1. **Onboard:** Register Riders and Drivers.
2. **Update:** Drivers update their location and availability status.
3. **Search:** Rider requests a ride. System scans available drivers using a **Matching Strategy**.
4. **Book:** Driver accepts (or is auto-assigned). Ride object created with status `BOOKED`.
5. **Travel:** Ride status updates to `IN_PROGRESS`.
6. **End:** Ride status updates to `FINISHED`. **Pricing Strategy** calculates the final fare.

---

### 3. Entities (Properties and Methods)

| Entity | Properties | Methods |
| --- | --- | --- |
| **Location** | `x` (double), `y` (double) | `getDistance(Location other)` |
| **User** (Base) | `id`, `name`, `email` | - |
| **Rider** | *Extends User* | - |
| **Driver** | *Extends User*, `vehicleDetails`, `isAvailable`, `currentLocation` | `updateLocation()`, `toggleAvailability()` |
| **Ride** | `id`, `rider`, `driver`, `src`, `dest`, `status`, `fare` | `start()`, `end()` |

---

### 4. Relations (OOP & SOLID)

* **Inheritance:** `Rider` and `Driver` extend `User`.
* **Composition:** `Ride` contains `Rider` and `Driver`. `Driver` contains `Location`.
* **Interface Segregation:** Separate interfaces for `PricingStrategy` and `MatchingStrategy` so new logic doesn't break existing code.
* **Dependency Inversion:** Service layer depends on Repository *Interfaces*, not concrete classes (allowing easy DB swap).

---

### 5. Design Patterns

| Pattern | Usage |
| --- | --- |
| **Strategy Pattern** | **Pricing:** (Default vs. Surge). **Matching:** (Nearest Driver vs. Highest Rated). |
| **Observer Pattern** | (Optional) Notify Rider when Driver arrives (skipped for 1hr code). |
| **Repository Pattern** | Abstract data access (`RideRepository`) to switch between In-Memory and SQL easily. |
| **Singleton** | Managed by Spring Container (Service/Controller beans). |

---

### 6. In-Memory vs Database Selection

**In-Memory Design:**
We will use `ConcurrentHashMap` in our Repository implementations. This mimics a DB and ensures thread safety during the demo.

**Database Selection Logic (if moving to Prod):**

| Requirement | Recommended DB | Reason |
| --- | --- | --- |
| **Transactions (Rides/Payments)** | **PostgreSQL / MySQL** | ACID compliance is strict for financial/ride state data. Relational data (Ride \rightarrow User) fits well. |
| **Location Tracking (Live)** | **Redis / GeoSpatial DB** | High write throughput needed for location pings. Redis Geo commands are optimized for "Nearest Neighbor" search. |
| **Ride History/Logs** | **Cassandra** | Massive scale writes for historical data that is rarely updated after creation. |

---

### 7. Architecture

**MVC (Model-View-Controller)** in Spring Boot.

* **Controller:** Handles HTTP requests.
* **Service:** Business logic (Matching, Fare calculation).
* **Repository:** Data access (In-memory Maps).

---

### 8. Important Dependencies

| Dependency | Why? |
| --- | --- |
| `spring-boot-starter-web` | Core REST API support and DI container. |
| `lombok` | Reduces boilerplate (Getters, Setters, Constructors) to save time in interview. |

---

### 9. Code Implementation

**Sequence:**
`Entities` \rightarrow `Interfaces (Strategies/Repos)` \rightarrow `Implementations` \rightarrow `Service` \rightarrow `Controller` \rightarrow `Main`

#### A. Entities & DTOs

```java

// 1. Common Value Object
@Data
@AllArgsConstructor
public class Location {
    private double x;
    private double y;

    public double distance(Location other) {
        return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }
}

// 2. Base User
@Data
@AllArgsConstructor
public abstract class User {
    private String id;
    private String name;
}

// 3. Concrete Users
@Data
public class Rider extends User {
    public Rider(String id, String name) { super(id, name); }
}

@Data
public class Driver extends User {
    private boolean available;
    private Location currentLocation;

    public Driver(String id, String name, Location loc) {
        super(id, name);
        this.currentLocation = loc;
        this.available = true;
    }
}

// 4. Ride Entity
@Data
public class Ride {
    private String id;
    private Rider rider;
    private Driver driver;
    private Location source;
    private Location destination;
    private RideStatus status;
    private double fare;

    public enum RideStatus { REQUESTED, BOOKED, IN_PROGRESS, ENDED }
}

```

#### B. Strategies (Interfaces & Impl)

```java

// Pricing Strategy
public interface PricingStrategy {
    double calculateFare(Location src, Location dest);
}

@Component
class DefaultPricingStrategy implements PricingStrategy {
    private static final double PER_KM_RATE = 10.0;
    @Override
    public double calculateFare(Location src, Location dest) {
        return src.distance(dest) * PER_KM_RATE;
    }
}

// Driver Matching Strategy
public interface DriverMatchingStrategy {
    Optional<Driver> findDriver(Rider rider, Location src, List<Driver> availableDrivers);
}

@Component
class NearestDriverStrategy implements DriverMatchingStrategy {
    @Override
    public Optional<Driver> findDriver(Rider rider, Location src, List<Driver> availableDrivers) {
        return availableDrivers.stream()
                .min((d1, d2) -> Double.compare(d1.getCurrentLocation().distance(src), 
                                                d2.getCurrentLocation().distance(src)));
    }
}

```

#### C. Repositories (Interface + In-Memory Impl)

```java

// Interface allows swapping with JpaRepository later
public interface DriverRepository {
    void save(Driver driver);
    List<Driver> findAvailableDrivers();
}

public interface RideRepository {
    void save(Ride ride);
    Ride findById(String id);
}

@Repository
class InMemoryDriverRepository implements DriverRepository {
    private final Map<String, Driver> store = new ConcurrentHashMap<>();

    @Override
    public void save(Driver driver) { store.put(driver.getId(), driver); }

    @Override
    public List<Driver> findAvailableDrivers() {
        return store.values().stream()
                .filter(Driver::isAvailable)
                .collect(Collectors.toList());
    }
}

@Repository
class InMemoryRideRepository implements RideRepository {
    private final Map<String, Ride> store = new ConcurrentHashMap<>();

    @Override
    public void save(Ride ride) { store.put(ride.getId(), ride); }

    @Override
    public Ride findById(String id) { return store.get(id); }
}

```

#### D. Service Layer

```java

@Service
@RequiredArgsConstructor
public class RideService {

    private final DriverRepository driverRepo;
    private final RideRepository rideRepo;
    private final DriverMatchingStrategy matchingStrategy;
    private final PricingStrategy pricingStrategy;

    public void registerDriver(Driver driver) {
        driverRepo.save(driver);
    }

    public Ride bookRide(Rider rider, Location src, Location dest) {
        var availableDrivers = driverRepo.findAvailableDrivers();
        
        // Strategy Pattern to find driver
        Driver selectedDriver = matchingStrategy.findDriver(rider, src, availableDrivers)
                .orElseThrow(() -> new RuntimeException("No Cabs Available!"));

        // Lock driver (Simple boolean flag for LLD)
        selectedDriver.setAvailable(false); 
        driverRepo.save(selectedDriver);

        Ride ride = new Ride();
        ride.setId(UUID.randomUUID().toString());
        ride.setRider(rider);
        ride.setDriver(selectedDriver);
        ride.setSource(src);
        ride.setDestination(dest);
        ride.setStatus(Ride.RideStatus.BOOKED);
        
        rideRepo.save(ride);
        return ride;
    }

    public Ride endRide(String rideId) {
        Ride ride = rideRepo.findById(rideId);
        ride.setStatus(Ride.RideStatus.ENDED);
        
        // Strategy Pattern for Pricing
        double fare = pricingStrategy.calculateFare(ride.getSource(), ride.getDestination());
        ride.setFare(fare);
        
        // Free up driver
        Driver driver = ride.getDriver();
        driver.setAvailable(true);
        driver.setCurrentLocation(ride.getDestination()); // Update loc to drop-off
        driverRepo.save(driver);
        
        rideRepo.save(ride);
        return ride;
    }
}

```

#### E. Controller (Minimal for Context)

```java

@Controller
@RequiredArgsConstructor
public class RideController {
    // Usually @RestController with @PostMapping, but kept simple for Main demo
    private final RideService rideService;

    public Ride requestRide(Rider rider, Location src, Location dest) {
        return rideService.bookRide(rider, src, dest);
    }
}

```

#### F. Main Class (Simulation)

```java

@SpringBootApplication
public class UberApp {

    public static void main(String[] args) {
        // 1. Initialize Context
        ConfigurableApplicationContext context = SpringApplication.run(UberApp.class, args);
        
        // 2. Fetch Service
        RideService rideService = context.getBean(RideService.class);

        System.out.println("--- Uber System Demo Started ---");

        // 3. Seed Data
        Rider rider = new Rider("R1", "Alice");
        Driver d1 = new Driver("D1", "Bob", new Location(0, 0)); // At origin
        Driver d2 = new Driver("D2", "Charlie", new Location(10, 10)); // Far away

        rideService.registerDriver(d1);
        rideService.registerDriver(d2);
        System.out.println("Drivers Registered.");

        // 4. Book Ride
        Location src = new Location(1, 1);
        Location dest = new Location(5, 5);
        
        System.out.println("Requesting ride for Alice from (1,1) to (5,5)...");
        try {
            Ride ride = rideService.bookRide(rider, src, dest);
            System.out.println("Ride Booked! Ride ID: " + ride.getId());
            System.out.println("Assigned Driver: " + ride.getDriver().getName());

            // 5. End Ride
            System.out.println("Ride in progress...");
            Ride completedRide = rideService.endRide(ride.getId());
            System.out.println("Ride Ended. Fare: $" + completedRide.getFare());
            System.out.println("Driver " + completedRide.getDriver().getName() + " is now available at destination.");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

```

---

### 10. Optimization Ideas

1. **Geo-Hashing:** Instead of calculating distance for *all* drivers (O(N)), use a Geo-Spatial index (like Uber's H3 or Google S2) to query only drivers in nearby cells.
2. **Distributed Locking:** In a real distributed system, `synchronized` or boolean flags won't work. Use Redis Distributed Lock (Redlock) to ensure a driver isn't assigned to two rides simultaneously.
3. **Caching:** Cache active Driver locations in Redis (TTL ~ 15 seconds) to reduce database load.
4. **Async Matching:** If no driver is found immediately, push the request to a Kafka queue and have a matcher service retry asynchronously, notifying the user via WebSocket when found.

---
