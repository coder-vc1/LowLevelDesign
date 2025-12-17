---
# Low-Level Design (LLD) for Payment Processing System
---

## 1) Functional and Non-Functional Requirements

| Type               | Requirements                                                                                                                                                                                                                                                                                                    |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Functional**     | 1. **Process Payment:** Support multiple payment modes (Credit Card, UPI, etc.).<br><br>2. **Get Status:** Fetch transaction status using Transaction ID.<br><br>3. **Record Keeping:** Store transaction details for auditing purposes.<br><br>4. **Validation:** Perform basic validation of payment details. |
| **Non-Functional** | 1. **Consistency:** ACID compliance is mandatory (money cannot disappear).<br><br>2. **Extensibility:** Easily add new payment methods without changing core logic.<br><br>3. **Security:** Abstract and protect sensitive payment data (mocked here).                                                          |

---

### 2) Rough Flow of Program

1. Client submits `PaymentRequest` to Controller.
2. Controller passes request to `PaymentService`.
3. Service creates a `Transaction` entity with status `PENDING`.
4. Service uses a `Factory` to select the correct `PaymentStrategy` (e.g., CreditCard vs UPI).
5. Strategy interacts with (Mock) Gateway and returns success/failure.
6. Service updates `Transaction` status and saves to `Repository`.
7. Response is returned to Client.

### 3) Entity (Properties and Methods)

| Entity | Properties | Methods |
| --- | --- | --- |
| **Transaction** | `id`, `amount`, `currency`, `userId`, `status`, `methodType`, `createdAt` | `updateStatus()`, `getters/setters` |
| **User** | `userId`, `name`, `email` | `getters/setters` |
| **PaymentRequest (DTO)** | `amount`, `currency`, `userId`, `methodType`, `paymentDetails(Map)` | `validate()` |

### 4) Relations (OOPS & SOLID Principles)

* **Interface Segregation / Open-Closed:** `PaymentStrategy` interface allows adding new payment types without modifying existing service code.
* **Dependency Inversion:** `PaymentService` depends on `PaymentStrategy` interface, not concrete classes.
* **Single Responsibility:** `Repository` handles data access, `Service` handles flow, `Strategy` handles specific payment logic.

### 5) Design Patterns

| Pattern | Usage |
| --- | --- |
| **Strategy** | To handle different payment algorithms (Credit Card vs UPI) interchangeably at runtime. |
| **Factory** | To instantiate the correct `PaymentStrategy` based on the `PaymentMethodType` enum. |
| **Repository** | To abstract the data layer (In-memory `ConcurrentHashMap` vs Real DB). |
| **Singleton** | Default Spring Beans scope for Services and Controllers. |

### 6) Database Selection (In-Memory & Real World)

**In-Memory (Default):** `ConcurrentHashMap<String, Transaction>` (Thread-safe, O(1) access).

**Real World DB Choice:**

| DB Choice | Reason |
| --- | --- |
| **RDBMS (PostgreSQL)** | **Strong Consistency (ACID):** Payment systems cannot tolerate eventual consistency. Relational DBs ensure row-level locking and transaction integrity. Postgres also supports JSONB if we need flexible payment metadata. |



### 7) Important Dependencies

| Dependency | Why? |
| --- | --- |
| `spring-boot-starter-web` | Core REST API functionality and embedded Tomcat server. |
| `lombok` | Reduces boilerplate (Getters, Setters, Builders, Constructors) significantly for speed. |

### 8) Code Implementation

#### A. Entities and Enums

```java

// Enums
public enum PaymentStatus { PENDING, SUCCESS, FAILED }

public enum PaymentMethodType { CREDIT_CARD, UPI }

// Transaction Entity

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private Double amount;
    private String currency;
    private String userId;
    private PaymentMethodType methodType;
    private PaymentStatus status;
    private LocalDateTime timestamp;
}

// PaymentRequest DTO
@Data
public class PaymentRequest {
    private Double amount;
    private String currency;
    private String userId;
    private PaymentMethodType methodType;
    private java.util.Map<String, String> additionalDetails; // e.g. {"cardNumber": "1234"}
}

```

#### B. Strategy Pattern (Core Logic)

```java


public interface PaymentStrategy {
    boolean process(Double amount, Map<String, String> details);
}

// Implementations
import org.springframework.stereotype.Component;

@Component("CREDIT_CARD")
public class CreditCardStrategy implements PaymentStrategy {
    @Override
    public boolean process(Double amount, Map<String, String> details) {
        System.out.println("Processing Credit Card: " + details.get("cardNumber") + " for amount " + amount);
        // Mocking external gateway call
        return true; 
    }
}

@Component("UPI")
public class UpiStrategy implements PaymentStrategy {
    @Override
    public boolean process(Double amount, Map<String, String> details) {
        System.out.println("Processing UPI to VPA: " + details.get("vpa") + " for amount " + amount);
        return true;
    }
}

```

#### C. Factory Pattern

```java

@Component
public class PaymentStrategyFactory {
    
    private final Map<String, PaymentStrategy> strategies;

    // Spring injects all beans implementing PaymentStrategy into this Map
    // Key is the bean name (e.g., "CREDIT_CARD" if named strictly, or "creditCardStrategy")
    public PaymentStrategyFactory(Map<String, PaymentStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentStrategy getStrategy(PaymentMethodType type) {
        // Assuming bean names match enum names or mapped manually
        // For simplicity in interview, rely on bean naming convention
        return strategies.get(type.name());
    }
}

```

#### D. Repository (In-Memory)

```java

@Repository
public class TransactionRepository {
    private final ConcurrentHashMap<String, Transaction> db = new ConcurrentHashMap<>();

    public Transaction save(Transaction txn) {
        db.put(txn.getId(), txn);
        return txn;
    }

    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(db.get(id));
    }
}

```

#### E. Service Layer

```java

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final TransactionRepository repository;
    private final PaymentStrategyFactory strategyFactory;

    public Transaction initiatePayment(PaymentRequest request) {
        // 1. Create Initial Record (PENDING)
        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .userId(request.getUserId())
                .methodType(request.getMethodType())
                .status(PaymentStatus.PENDING)
                .timestamp(LocalDateTime.now())
                .build();
        repository.save(txn);

        // 2. Process Payment via Strategy
        PaymentStrategy strategy = strategyFactory.getStrategy(request.getMethodType());
        boolean success = strategy.process(request.getAmount(), request.getAdditionalDetails());

        // 3. Update Status
        txn.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        return repository.save(txn);
    }
}

```

#### F. Controller

```java

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;

    @PostMapping
    public Transaction pay(@RequestBody PaymentRequest request) {
        return service.initiatePayment(request);
    }
}

```

#### G. Main Class (Demo Simulation)

```java

@SpringBootApplication
public class PaymentApp {

    public static void main(String[] args) {
        // 1. Initialize Context
        ConfigurableApplicationContext context = SpringApplication.run(PaymentApp.class, args);
        
        // 2. Fetch Service manually
        PaymentService service = context.getBean(PaymentService.class);

        System.out.println("----- STARTING SIMULATION -----");

        // 3. Simulate Credit Card Payment
        PaymentRequest req1 = new PaymentRequest();
        req1.setAmount(100.50);
        req1.setCurrency("USD");
        req1.setUserId("User_1");
        req1.setMethodType(PaymentMethodType.CREDIT_CARD);
        Map<String, String> cardDetails = new HashMap<>();
        cardDetails.put("cardNumber", "4111-2222-3333-4444");
        req1.setAdditionalDetails(cardDetails);

        Transaction txn1 = service.initiatePayment(req1);
        System.out.println("Result 1: " + txn1);

        // 4. Simulate UPI Payment
        PaymentRequest req2 = new PaymentRequest();
        req2.setAmount(500.00);
        req2.setCurrency("INR");
        req2.setUserId("User_2");
        req2.setMethodType(PaymentMethodType.UPI);
        Map<String, String> upiDetails = new HashMap<>();
        upiDetails.put("vpa", "john@upi");
        req2.setAdditionalDetails(upiDetails);

        Transaction txn2 = service.initiatePayment(req2);
        System.out.println("Result 2: " + txn2);
        
        System.out.println("----- SIMULATION END -----");
    }
}

```

### 10) Optimization Ideas

* **Idempotency:** Add a UUID `Idempotency-Key` in the request header to prevent duplicate charges if the client retries due to a timeout.
* **Message Queue (Async):** Push payment events to Kafka/RabbitMQ. This decouples the user response from the actual bank processing if the bank is slow.
* **Security:** Tokenize card numbers (PCI-DSS) and never log raw sensitive details.

