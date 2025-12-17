Here is the Low-Level Design (LLD) for a URL Shortener, structured specifically for a 1-hour interview setting using Java Spring Boot.

### 1\) Rough Flow of Program

1.  **Input:** Client sends a Long URL (e.g., `https://google.com/very-long-path`).
2.  **Processing:**
      * System validates the URL.
      * System generates a unique unique ID or Hash.
      * System encodes this ID into a Short Code (e.g., `abc12`).
3.  **Storage:** System maps `Short Code` \<-\> `Long URL` in the persistence layer.
4.  **Output:** System returns the Short URL (e.g., `http://short.ly/abc12`).
5.  **Retrieval:** Client hits Short URL -\> System looks up Long URL -\> System redirects (HTTP 302).

-----

### 2\) Functional & Non-Functional Requirements

| Type | Requirement |
| :--- | :--- |
| **Functional** | 1. Shorten a long URL.<br>2. Redirect from short code to original URL.<br>3. Handle invalid URLs.<br>4. (Optional) Custom alias support. |
| **Non-Functional** | 1. **Read-Heavy:** Redirections happen much more than shortening (100:1 ratio).<br>2. **Low Latency:** Redirection must be near-instant.<br>3. **Unique Keys:** No two different URLs should get the same short code collision.<br>4. **Scalable:** Handle traffic spikes. |

-----

### 3\) Entity (Properties and Methods)

We keep entities lightweight (POJO).

**Class:** `UrlMapping`

  * **Properties:**
      * `id` (Long): Unique database ID (primary key).
      * `longUrl` (String): The original URL.
      * `shortCode` (String): The generated unique string (e.g., "x7z").
      * `createdDate` (LocalDateTime): Audit.
  * **Methods:**
      * Standard Getters/Setters.
      * `isValid()`: Helper to check if URL format is correct (optional inside entity, usually in DTO/Service).

-----

### 4\) Relations (OOPS & SOLID Principles)

  * **Single Responsibility Principle (SRP):**
      * `Controller`: Handles HTTP requests/responses only.
      * `Service`: Handles the business logic (Base62 encoding, ID generation).
      * `Repository`: Handles data storage/retrieval.
  * **Dependency Inversion (DIP):**
      * The Service depends on the `UrlRepository` **interface**, not the concrete `InMemoryUrlRepository`. This makes switching to a real DB seamless.
  * **Interface Segregation:**
      * Repository interfaces are specific to data operations.

-----

### 5\) Design Patterns

  * **Repository Pattern:** To abstract the data layer (In-Memory vs. MySQL vs. Redis).
  * **Singleton Pattern:** Spring Beans (`@Service`, `@Component`) are singletons by default.
  * **Strategy Pattern (Implicit):** The logic to generate the short code (Base62 vs. MD5 vs. Random) can be swapped easily in the Service layer.

-----

### 6\) Storage Strategy (In-Memory & DB Selection)

**In-Memory Approach:**
We will use a `ConcurrentHashMap` for storage and an `AtomicLong` to simulate a database auto-increment sequence.

**Database Selection Logic:**

| Database Type | Recommended? | Reason |
| :--- | :--- | :--- |
| **NoSQL (Key-Value)**<br>*(e.g., Redis, DynamoDB)* | **Highly Recommended** | 1. The data model is simple (Key -\> Value).<br>2. Extremely fast lookups (O(1)).<br>3. Horizontally scalable for billions of records. |
| **RDBMS**<br>*(e.g., PostgreSQL, MySQL)* | **Acceptable** | 1. Good if you need strict ACID compliance or complex relations (User -\> URLs).<br>2. Can use standard Auto-Increment ID for Base62 encoding easily. |
| **Why not Graph DB?** | No | No complex relationships between data nodes exist here. |

-----

### 7\) Architecture (MVC)

[Image of MVC Architecture Diagram]

**Structure:**

  * **Controller:** `UrlShortenerController`
  * **Service:** `UrlShortenerService` (Business Logic)
  * **Repository:** `UrlRepository` (Interface) -\> `InMemoryUrlRepository` (Impl)

-----

### 8\) Important Dependencies

| Dependency | Purpose |
| :--- | :--- |
| `spring-boot-starter-web` | Provides REST API framework (Tomcat, MVC). |
| `lombok` | Reduces boilerplate code (Getters, Setters, Constructors). Helps write code faster in interviews. |
| `commons-validator` (Optional) | For strictly validating if the input string is a real URL. |

-----


### 9\) Optimization Ideas (No Code)

If asked "How do we scale this?", provide these points:

1.  **Caching (Redis):** Place Redis before the DB. Since reads are 99% of traffic, check Cache first. If miss, check DB and update Cache.
2.  **Key Generation Service (KGS):** Instead of calculating Base62 on the fly (which relies on a single ID counter and is a bottleneck), pre-generate millions of unique 6-character keys and store them in a "Unused Key" table. When a user requests, just pop one off the stack.
3.  **Database Sharding:** Shard the database based on the first character of the short code or hash of the user ID to distribute load.
4.  **Analytics:** Use Kafka to async push click events (analytics) to a data warehouse, so writing stats doesn't slow down the redirection speed.

-----
### 10\) Code Implementation

#### A. Entity & DTOs

```java

// Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlMapping {
    private Long id;
    private String longUrl;
    private String shortCode;
    private LocalDateTime createdDate;
}

// DTO Request
@Data
class ShortenRequest {
    private String longUrl;
}

// DTO Response
@Data
@AllArgsConstructor
class ShortenResponse {
    private String shortUrl;
    private String shortCode;
}
```

#### B. Repository (Interface & In-Memory Implementation)

```java

// Interface allows swapping DB later
public interface UrlRepository {
    UrlMapping save(UrlMapping mapping);
    Optional<UrlMapping> findByShortCode(String shortCode);
    Long getNextId(); // Simulates DB Sequence
}

@Repository
class InMemoryUrlRepository implements UrlRepository {
    // Thread-safe map for storage
    private final Map<String, UrlMapping> storage = new ConcurrentHashMap<>();
    
    // Thread-safe counter for IDs
    private final AtomicLong sequence = new AtomicLong(10000); // Start at 10k to have non-trivial IDs

    @Override
    public UrlMapping save(UrlMapping mapping) {
        // In real DB, ID is generated on save. Here we set it manually if null.
        if(mapping.getId() == null) {
            mapping.setId(getNextId());
        }
        storage.put(mapping.getShortCode(), mapping);
        return mapping;
    }

    @Override
    public Optional<UrlMapping> findByShortCode(String shortCode) {
        return Optional.ofNullable(storage.get(shortCode));
    }
    
    @Override
    public Long getNextId() {
        return sequence.incrementAndGet();
    }
}
```

#### C. Service (Business Logic)

This uses **Base62 Encoding**. This converts a numeric ID (10001) into a string (e.g., "cbA"). This is the most efficient way to generate short, unique codes.

```java

@Service
public class UrlShortenerService {

    private final UrlRepository repository;
    private static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final char[] CHAR_MAP = ALLOWED_CHARS.toCharArray();
    private static final int BASE = ALLOWED_CHARS.length(); // 62
    private static final String DOMAIN = "http://short.ly/";

    public UrlShortenerService(UrlRepository repository) {
        this.repository = repository;
    }

    public String shortenUrl(String longUrl) {
        // 1. Get a unique ID (Simulating DB Auto Increment)
        Long id = repository.getNextId();
        
        // 2. Encode ID to Base62 Short Code
        String shortCode = encodeBase62(id);
        
        // 3. Save to DB
        UrlMapping mapping = new UrlMapping(id, longUrl, shortCode, LocalDateTime.now());
        repository.save(mapping);
        
        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(UrlMapping::getLongUrl)
                .orElseThrow(() -> new RuntimeException("URL not found for code: " + shortCode));
    }

    // Algorithm: Base 10 (ID) -> Base 62 (String)
    private String encodeBase62(long id) {
        StringBuilder sb = new StringBuilder();
        if (id == 0) return String.valueOf(CHAR_MAP[0]);
        
        while (id > 0) {
            int remainder = (int) (id % BASE);
            sb.append(CHAR_MAP[remainder]);
            id = id / BASE;
        }
        return sb.reverse().toString();
    }
}
```

#### D. Controller

```java

@RestController
@RequestMapping("/api")
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    // API to Shorten
    @PostMapping("/shorten")
    public ResponseEntity<String> shorten(@RequestBody String longUrl) {
        String shortCode = service.shortenUrl(longUrl);
        return ResponseEntity.ok("http://short.ly/" + shortCode);
    }

    // API to Redirect
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = service.getOriginalUrl(shortCode);
        
        // Return 302 Found (Temporary Redirect)
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
```

#### E. Main Class (Simulation)

```java

@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(String[] args) {
        // 1. Initialize Spring Context
        ConfigurableApplicationContext context = SpringApplication.run(UrlShortenerApplication.class, args);

        // 2. Manually fetch the Service Bean
        UrlShortenerService service = context.getBean(UrlShortenerService.class);

        System.out.println("---- STARTING SIMULATION ----");

        // 3. Simulate User Input
        String originalUrl = "https://www.google.com/search?q=system+design+interview";
        System.out.println("Original URL: " + originalUrl);

        // 4. Shorten Logic
        String shortCode = service.shortenUrl(originalUrl);
        System.out.println("Generated Short Code: " + shortCode);
        System.out.println("Full Short URL: http://short.ly/" + shortCode);

        // 5. Redirect/Retrieval Logic
        String retrievedUrl = service.getOriginalUrl(shortCode);
        System.out.println("Retrieved URL from DB: " + retrievedUrl);

        if (originalUrl.equals(retrievedUrl)) {
            System.out.println("SUCCESS: URL mapping works correctly.");
        } else {
            System.out.println("FAILURE: URLs do not match.");
        }
        
        System.out.println("---- END SIMULATION ----");
    }
}
```



