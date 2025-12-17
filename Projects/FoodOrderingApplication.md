---
# Low-Level Design (LLD) for a Food Ordering System using Java Spring Boot
---

## 1) Functional and Non-Functional Requirements

| Type               | Requirements                                                                                                                                                                                                                                                         |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Functional**     | 1. **User Management:** Register / Login.<br><br>2. **Restaurant Search:** View restaurants and menus.<br><br>3. **Cart & Order:** Add items to cart and place order.<br><br>4. **Order Execution:** Update order status (Preparing → Out for Delivery → Delivered). |
| **Non-Functional** | 1. **Concurrency:** Handle multiple users placing orders simultaneously.<br><br>2. **Low Latency:** Fast menu retrieval.<br><br>3. **Extensibility:** Easily swap In-Memory DB with SQL / NoSQL databases.                                                           |

---

### 2) Rough Flow of Program

1. **Init:** System starts, valid Restaurants and Menus are pre-loaded.
2. **User Action:** User selects a Restaurant -> Selects Items.
3. **Order Creation:** User sends `PlaceOrderRequest`.
4. **Validation:** System checks item availability and price.
5. **Processing:** Order created with status `PLACED`. Payment processed (Mock).
6. **Updates:** Restaurant updates status -> `PREPARING` -> `DELIVERED`.

---

### 3) Entities (Properties & Methods)

| Entity | Properties | Key Methods |
| --- | --- | --- |
| **User** | `id`, `name`, `phone` | `updateProfile()` |
| **Restaurant** | `id`, `name`, `menu (List<MenuItem>)` | `addMenuItem()`, `removeMenuItem()` |
| **MenuItem** | `id`, `name`, `price`, `available` | `updatePrice()` |
| **Order** | `id`, `userId`, `restaurantId`, `totalAmount`, `status`, `items` | `calculateTotal()`, `updateStatus()` |

---

### 4) Relations (OOPS & SOLID)

* **User** `1 : N` **Order** (Association)
* **Restaurant** `1 : N` **MenuItem** (Composition)
* **Order** `1 : N` **OrderItem** (Composition)
* **Repository Pattern (DIP):** High-level modules (Service) depend on abstractions (Interfaces), not concrete classes (In-Memory Map).

---

### 5) Design Patterns

| Pattern | Usage in this Design |
| --- | --- |
| **Singleton** | Spring Beans (Service, Repository, Controller) are singletons by default. |
| **Strategy** | (Optional but good to mention) For **PricingStrategy** (Surge pricing vs Flat) or **PaymentStrategy**. |
| **Factory** | Hidden within Spring's Dependency Injection (IOC Container). |
| **Builder** | Used (via Lombok `@Builder`) to construct complex `Order` objects cleanly. |

---

### 6) Data Storage Strategy

**In-Memory (Default):**

* We use `ConcurrentHashMap<Long, Entity>` to simulate database tables.
* We use `AtomicLong` for auto-incrementing IDs.
* **Why?** Thread-safe and fast for the 1-hour interview simulation.

**Database Selection (If moving to Production):**

| Database Type | Recommendation | Reason |
| --- | --- | --- |
| **RDBMS (PostgreSQL/MySQL)** | **Recommended** | **ACID properties are non-negotiable for billing and order status.** We need strict consistency (User A and User B cannot buy the last item simultaneously). |
| **NoSQL (MongoDB)** | Secondary | Good for storing the *Menu* (Catalog) as it has flexible schema and high read throughput, but not for the transactional Order table. |

---

### 7) Architecture

We follow **MVC (Model-View-Controller)** layered architecture:
`Controller (API Layer)` -> `Service (Business Logic)` -> `Repository (Data Access)`

---

### 8) Important Dependencies

| Dependency | Purpose |
| --- | --- |
| **Spring Boot Starter Web** | Provides REST API framework and Embedded Tomcat. |
| **Lombok** | Removes boilerplate (Getters, Setters, Constructors, Builder). |
| **Spring Boot Starter Test** | For Unit testing (if time permits). |

---

### 9) Code Implementation

#### A. Entities

```java

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class User {
    private Long id;
    private String name;
}

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class MenuItem {
    private Long id;
    private String name;
    private double price;
}

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Restaurant {
    private Long id;
    private String name;
    private List<MenuItem> menu;
}

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class Order {
    private Long id;
    private Long userId;
    private Long restaurantId;
    private List<MenuItem> items;
    private double totalAmount;
    private OrderStatus status;
}

public enum OrderStatus {
    PLACED, PREPARING, DELIVERED, CANCELLED
}

```

#### B. DTOs (Data Transfer Objects)

```java

@Data
public class OrderRequest {
    private Long userId;
    private Long restaurantId;
    private List<Long> menuItemIds;
}

```

#### C. Repository Layer (Interface + In-Memory Impl)

```java

// Generic interface to allow DB switch later
public interface FoodRepository {
    User saveUser(User user);
    Restaurant saveRestaurant(Restaurant restaurant);
    Order saveOrder(Order order);
    Optional<Restaurant> findRestaurantById(Long id);
    Optional<User> findUserById(Long id);
    List<Order> findAllOrders();
}

@Repository
class InMemoryFoodRepository implements FoodRepository {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, Restaurant> restaurants = new ConcurrentHashMap<>();
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    
    private final AtomicLong userIdGen = new AtomicLong(1);
    private final AtomicLong orderIdGen = new AtomicLong(1);
    private final AtomicLong restIdGen = new AtomicLong(1);

    @Override
    public User saveUser(User user) {
        if(user.getId() == null) user.setId(userIdGen.getAndIncrement());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Restaurant saveRestaurant(Restaurant restaurant) {
        if(restaurant.getId() == null) restaurant.setId(restIdGen.getAndIncrement());
        restaurants.put(restaurant.getId(), restaurant);
        return restaurant;
    }

    @Override
    public Order saveOrder(Order order) {
        if(order.getId() == null) order.setId(orderIdGen.getAndIncrement());
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Restaurant> findRestaurantById(Long id) {
        return Optional.ofNullable(restaurants.get(id));
    }

    @Override
    public Optional<User> findUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }
    
    @Override
    public List<Order> findAllOrders() {
        return new ArrayList<>(orders.values());
    }
}

```

#### D. Service Layer

```java

@Service
public class FoodOrderingService {

    private final FoodRepository repository;

    @Autowired
    public FoodOrderingService(FoodRepository repository) {
        this.repository = repository;
    }

    // --- Data Seeding for Demo ---
    public void seedData() {
        User u = User.builder().name("Alice").build();
        repository.saveUser(u);

        MenuItem m1 = MenuItem.builder().id(1L).name("Burger").price(100.0).build();
        MenuItem m2 = MenuItem.builder().id(2L).name("Pizza").price(200.0).build();
        
        Restaurant r = Restaurant.builder()
                .name("Tasty Bites")
                .menu(List.of(m1, m2))
                .build();
        repository.saveRestaurant(r);
        
        System.out.println("Data Seeded: User ID " + u.getId() + ", Restaurant ID " + r.getId());
    }

    // --- Core Logic ---
    public Order placeOrder(OrderRequest request) {
        User user = repository.findUserById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Restaurant restaurant = repository.findRestaurantById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        List<MenuItem> orderedItems = new ArrayList<>();
        double total = 0;

        // Naive search O(N*M) - Optimization: Use Map for menu items
        for (Long itemId : request.getMenuItemIds()) {
            MenuItem item = restaurant.getMenu().stream()
                    .filter(m -> m.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Item not in menu"));
            orderedItems.add(item);
            total += item.getPrice();
        }

        Order order = Order.builder()
                .userId(user.getId())
                .restaurantId(restaurant.getId())
                .items(orderedItems)
                .totalAmount(total)
                .status(OrderStatus.PLACED)
                .build();

        return repository.saveOrder(order);
    }
    
    public List<Order> getAllOrders() {
        return repository.findAllOrders();
    }
}

```

#### E. Controller Layer

```java

@RestController
@RequestMapping("/api/food")
public class FoodController {

    @Autowired
    private FoodOrderingService service;

    @PostMapping("/order")
    public Order createOrder(@RequestBody OrderRequest request) {
        return service.placeOrder(request);
    }
    
    @GetMapping("/orders")
    public List<Order> getOrders() {
        return service.getAllOrders();
    }
}

```

#### F. Main Method (Simulation Demo)

```java

@SpringBootApplication
public class FoodOrderingApplication {

    public static void main(String[] args) {
        // 1. Initialize Spring Context
        ConfigurableApplicationContext context = SpringApplication.run(FoodOrderingApplication.class, args);

        // 2. Manual Bean Retrieval
        FoodOrderingService service = context.getBean(FoodOrderingService.class);

        // 3. Simulation Logic
        System.out.println("---------- STARTING DEMO ----------");
        
        // Seed Initial Data
        service.seedData();

        // Create an Order Request (User 1, Rest 1, Items [1, 2])
        OrderRequest request = new OrderRequest();
        request.setUserId(1L);
        request.setRestaurantId(1L);
        request.setMenuItemIds(List.of(1L, 2L)); // Burger + Pizza

        // Place Order
        try {
            Order order = service.placeOrder(request);
            System.out.println("Success! Order Placed: " + order);
        } catch (Exception e) {
            System.err.println("Order Failed: " + e.getMessage());
        }

        System.out.println("All Orders in System: " + service.getAllOrders());
        System.out.println("---------- DEMO ENDED ----------");
        
        // Note: Do not close context if you want the Web Server to remain running for API testing
        // context.close(); 
    }
}

```

---

### 10) Optimization Ideas (Untouched)

1. **Caching:** Use Redis for Restaurant Menus (Read-heavy data).
2. **Indexing:** Database indexing on `restaurant_id` and `user_id` for faster lookups.
3. **Async Processing:** Move "Payment Processing" and "Notification" to a Message Queue (Kafka/RabbitMQ) so the user gets an immediate response.
4. **Pagination:** Implement pagination for fetching Menu Items and Past Orders.

Would you like me to detail the **JUnit Test cases** for the Service layer next?
