# Ticket Booking System

A Spring Boot-based ticket booking system that handles concurrent ticket bookings with support for async processing, transaction management, and optimistic locking.

## Technology Stack

- **Framework**: Spring Boot 3.5.3
- **Language**: Java 21
- **Database**: H2 (In-memory)
- **ORM**: Spring Data JPA
- **Security**: Spring Security
- **Build Tool**: Maven
- **Additional Features**: Async Processing, Transaction Management

## Project Structure

```
ticketBooking/
├── src/main/java/com/bookingSystem/ticketBooking/
│   ├── config/
│   │   ├── DataLoader.java              # Initial data setup
│   │   ├── SecurityConfig.java          # Security configuration
│   │   └── ThreadPoolConfig.java        # Async thread pool config
│   ├── controller/
│   │   ├── BookingController.java       # Booking REST endpoints
│   │   ├── EventController.java         # Event REST endpoints
│   │   └── UserController.java          # User REST endpoints
│   ├── entity/
│   │   ├── Booking.java                 # Booking entity with status enum
│   │   ├── Event.java                   # Event entity with versioning
│   │   └── User.java                    # User entity
│   ├── repository/
│   │   ├── BookingRepository.java       # Booking data access
│   │   ├── EventRepository.java         # Event data access with custom queries
│   │   └── UserRepository.java          # User data access
│   ├── service/
│   │   ├── BookingService.java          # Booking business logic
│   │   ├── EventService.java            # Event business logic
│   │   └── UserService.java             # User business logic
│   └── TicketBookingApplication.java    # Main application class
├── src/main/resources/
│   ├── application.properties           # Application configuration
│   └── static/                         # Static web resources
├── src/test/java/
│   ├── ConcurrentBookingTest.java       # Concurrent booking tests
│   └── TicketBookingApplicationTests.java
└── pom.xml                             # Maven configuration
```

## UML Class Diagram

```mermaid
classDiagram
    class User {
        -Long id
        -String username
        -String password
        -String email
        -String role
        -List~Booking~ bookings
        +getId() Long
        +getUsername() String
        +getPassword() String
        +getEmail() String
        +getRole() String
        +getBookings() List~Booking~
    }

    class Event {
        -Long id
        -String name
        -String venue
        -LocalDateTime eventDate
        -Integer totalSeats
        -Integer availableSeats
        -Double price
        -List~Booking~ bookings
        -Long version
        +getId() Long
        +getName() String
        +getVenue() String
        +getEventDate() LocalDateTime
        +getTotalSeats() Integer
        +getAvailableSeats() Integer
        +getPrice() Double
        +getBookings() List~Booking~
        +getVersion() Long
    }

    class Booking {
        -Long id
        -User user
        -Event event
        -Integer seatsBooked
        -LocalDateTime bookingDate
        -Double totalAmount
        -BookingStatus status
        +getId() Long
        +getUser() User
        +getEvent() Event
        +getSeatsBooked() Integer
        +getBookingDate() LocalDateTime
        +getTotalAmount() Double
        +getStatus() BookingStatus
    }

    class BookingStatus {
        <<enumeration>>
        CONFIRMED
        CANCELLED
        PENDING
    }

    class UserService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        +getAllUsers() List~User~
        +getUserById(Long) Optional~User~
        +getUserByUsername(String) Optional~User~
        +createUser(User) User
        +updateUser(User) User
        +deleteUser(Long) void
        +existsByUsername(String) boolean
        +existsByEmail(String) boolean
    }

    class EventService {
        -EventRepository eventRepository
        +getAllEvents() List~Event~
        +getAvailableEvents() List~Event~
        +getEventById(Long) Optional~Event~
        +searchEventsByName(String) List~Event~
        +createEvent(Event) Event
        +updateEvent(Event) Event
        +deleteEvent(Long) void
        +getEventByIdWithLock(Long) Optional~Event~
    }

    class BookingService {
        -BookingRepository bookingRepository
        -EventRepository eventRepository
        -UserRepository userRepository
        -ConcurrentHashMap eventLocks
        +bookTicket(Long, Long, Integer) Booking
        +bookTicketAsync(Long, Long, Integer) CompletableFuture~Booking~
        +getAllBookings() List~Booking~
        +getBookingById(Long) Optional~Booking~
        +getUserBookings(Long) List~Booking~
        +getEventBookings(Long) List~Booking~
        +cancelBooking(Long) boolean
        +getBookingCountForEvent(Long) Long
    }

    class UserController {
        -UserService userService
        +getAllUsers() ResponseEntity
        +getUserById(Long) ResponseEntity
        +createUser(User) ResponseEntity
        +updateUser(Long, User) ResponseEntity
        +deleteUser(Long) ResponseEntity
    }

    class EventController {
        -EventService eventService
        +getAllEvents() ResponseEntity
        +getAvailableEvents() ResponseEntity
        +getEventById(Long) ResponseEntity
        +searchEvents(String) ResponseEntity
        +createEvent(Event) ResponseEntity
        +updateEvent(Long, Event) ResponseEntity
        +deleteEvent(Long) ResponseEntity
    }

    class BookingController {
        -BookingService bookingService
        +getAllBookings() ResponseEntity
        +getBookingById(Long) ResponseEntity
        +bookTicket(BookingRequest) ResponseEntity
        +bookTicketAsync(BookingRequest) CompletableFuture
        +getUserBookings(Long) ResponseEntity
        +getEventBookings(Long) ResponseEntity
        +cancelBooking(Long) ResponseEntity
        +getBookingCountForEvent(Long) ResponseEntity
    }

    class UserRepository {
        <<interface>>
        +findByUsername(String) Optional~User~
        +existsByUsername(String) boolean
        +existsByEmail(String) boolean
    }

    class EventRepository {
        <<interface>>
        +findAvailableEvents() List~Event~
        +findByNameContaining(String) List~Event~
        +findByIdWithLock(Long) Optional~Event~
    }

    class BookingRepository {
        <<interface>>
        +findByUserId(Long) List~Booking~
        +findByEventId(Long) List~Booking~
        +findByUserUsername(String) List~Booking~
        +countByEventId(Long) Long
    }

    %% Entity Relationships
    User ||--o{ Booking : "has many"
    Event ||--o{ Booking : "has many"
    Booking ||--|| User : "belongs to"
    Booking ||--|| Event : "belongs to"
    Booking ||--|| BookingStatus : "has status"

    %% Service Dependencies
    UserService --> UserRepository : "uses"
    EventService --> EventRepository : "uses"
    BookingService --> BookingRepository : "uses"
    BookingService --> EventRepository : "uses"
    BookingService --> UserRepository : "uses"

    %% Controller Dependencies
    UserController --> UserService : "uses"
    EventController --> EventService : "uses"
    BookingController --> BookingService : "uses"

    %% Repository Relationships
    UserRepository -.-> User : "manages"
    EventRepository -.-> Event : "manages"
    BookingRepository -.-> Booking : "manages"
```

## Key Features

### 🎫 Core Booking System
- **User Management**: Registration, authentication, and user roles
- **Event Management**: Create, update, and manage events with seat availability
- **Booking System**: Reserve tickets with real-time seat availability updates

### 🔒 Concurrency Control
- **Optimistic Locking**: Version-based locking on Event entities
- **Pessimistic Locking**: ReentrantLock for critical booking sections
- **Async Processing**: Non-blocking ticket booking with CompletableFuture

### 🛡️ Security & Data Integrity
- **Spring Security**: Authentication and authorization
- **Transaction Management**: ACID compliance for booking operations
- **Password Encryption**: BCrypt password encoding
- **Input Validation**: Entity-level and service-level validation

### 📊 Advanced Features
- **Booking Status Management**: CONFIRMED, CANCELLED, PENDING states
- **Seat Availability Tracking**: Real-time seat count updates
- **Concurrent Booking Support**: Handle multiple simultaneous bookings
- **Booking History**: Track user booking history and event bookings

## Database Schema

### Entities
- **Users Table**: User authentication and profile information
- **Events Table**: Event details with seat management and versioning
- **Bookings Table**: Booking records linking users to events

### Relationships
- User (1) ←→ (N) Booking: One user can have multiple bookings
- Event (1) ←→ (N) Booking: One event can have multiple bookings
- Booking (N) ←→ (1) User: Each booking belongs to one user
- Booking (N) ←→ (1) Event: Each booking is for one event

## API Endpoints

### User Management
```
GET    /api/users           - Get all users
GET    /api/users/{id}      - Get user by ID
POST   /api/users           - Create new user
PUT    /api/users/{id}      - Update user
DELETE /api/users/{id}      - Delete user
```

### Event Management
```
GET    /api/events              - Get all events
GET    /api/events/available    - Get available events
GET    /api/events/{id}         - Get event by ID
GET    /api/events/search       - Search events by name
POST   /api/events              - Create new event
PUT    /api/events/{id}         - Update event
DELETE /api/events/{id}         - Delete event
```

### Booking Management
```
GET    /api/bookings                    - Get all bookings
GET    /api/bookings/{id}               - Get booking by ID
POST   /api/bookings/book               - Create booking (synchronous)
POST   /api/bookings/book-async         - Create booking (asynchronous)
GET    /api/bookings/user/{userId}      - Get user bookings
GET    /api/bookings/event/{eventId}    - Get event bookings
DELETE /api/bookings/{bookingId}        - Cancel booking
GET    /api/bookings/count/event/{eventId} - Get booking count for event
```

## Getting Started

### Prerequisites
- Java 21 or higher
- Maven 3.6+

### Installation
1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ticketBooking
   ```

2. **Build the project**
   ```bash
   ./mvnw clean install
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Access the application**
   - API Base URL: `http://localhost:8080`
   - H2 Console: `http://localhost:8080/h2-console`

### Configuration
- **Database**: H2 in-memory database (configured in `application.properties`)
- **Security**: Basic authentication enabled
- **Async Processing**: Custom thread pool configuration
- **JPA**: Hibernate as JPA provider with DDL auto-generation

## Testing

### Run Tests
```bash
./mvnw test
```

### Concurrent Booking Tests
The system includes specialized tests for concurrent booking scenarios to ensure thread safety and data integrity.

## Architecture Highlights

- **Layered Architecture**: Clear separation of concerns with Controller-Service-Repository pattern
- **Dependency Injection**: Spring's IoC container manages component lifecycle
- **Async Processing**: CompletableFuture for non-blocking operations
- **Transaction Management**: Declarative transactions with Spring's @Transactional
- **Concurrent Access Control**: Combination of optimistic and pessimistic locking strategies

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.