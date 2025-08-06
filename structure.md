# Ticket Booking System with Spring Boot - Complete Guide

## Overview
This guide will walk you through building a complete ticket booking system using Spring Boot, JPA, and Spring Security with multithreading support for concurrent bookings.

## Features
- User authentication and authorization
- Event management
- Concurrent ticket booking with thread safety
- Pessimistic and optimistic locking
- Async booking operations
- RESTful API endpoints

## Prerequisites
- Java 8 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)
- Basic knowledge of Spring Boot, JPA, and REST APIs

---

## Step 1: Project Setup

### 1.1 Create Spring Boot Project
Create a new Maven project or use Spring Initializr (https://start.spring.io/)

### 1.2 Configure pom.xml
Create the following `pom.xml` file in your project root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.ticketbooking</groupId>
    <artifactId>ticket-booking-system</artifactId>
    <version>1.0.0</version>
    <name>ticket-booking-system</name>
    <description>Ticket booking system with Spring Boot</description>
    
    <properties>
        <java.version>8</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Boot JPA Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- Spring Boot Security Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- H2 Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Spring Boot Test Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Spring Security Test -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Step 2: Application Configuration

### 2.1 Create application.properties
Create `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:ticketdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Security Configuration
spring.security.user.name=admin
spring.security.user.password=admin123
spring.security.user.roles=ADMIN

# Thread Pool Configuration
ticket.booking.thread-pool.core-size=5
ticket.booking.thread-pool.max-size=10
ticket.booking.thread-pool.queue-capacity=100

# Logging
logging.level.com.ticketbooking=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## Step 3: Entity Classes

### 3.1 Create User Entity
Create `src/main/java/com/ticketbooking/entity/User.java`:

```java
package com.ticketbooking.entity;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String role = "USER";
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings;
    
    // Constructors
    public User() {}
    
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }
}
```

### 3.2 Create Event Entity
Create `src/main/java/com/ticketbooking/entity/Event.java`:

```java
package com.ticketbooking.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String venue;
    
    @Column(nullable = false)
    private LocalDateTime eventDate;
    
    @Column(nullable = false)
    private Integer totalSeats;
    
    @Column(nullable = false)
    private Integer availableSeats;
    
    @Column(nullable = false)
    private Double price;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings;
    
    @Version
    private Long version; // For optimistic locking
    
    // Constructors
    public Event() {}
    
    public Event(String name, String venue, LocalDateTime eventDate, Integer totalSeats, Double price) {
        this.name = name;
        this.venue = venue;
        this.eventDate = eventDate;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.price = price;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    
    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
```

### 3.3 Create Booking Entity
Create `src/main/java/com/ticketbooking/entity/Booking.java`:

```java
package com.ticketbooking.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @Column(nullable = false)
    private Integer seatsBooked;
    
    @Column(nullable = false)
    private LocalDateTime bookingDate;
    
    @Column(nullable = false)
    private Double totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;
    
    // Constructors
    public Booking() {}
    
    public Booking(User user, Event event, Integer seatsBooked, Double totalAmount) {
        this.user = user;
        this.event = event;
        this.seatsBooked = seatsBooked;
        this.totalAmount = totalAmount;
        this.bookingDate = LocalDateTime.now();
        this.status = BookingStatus.CONFIRMED;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public Integer getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(Integer seatsBooked) { this.seatsBooked = seatsBooked; }
    
    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
    
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    
    public enum BookingStatus {
        CONFIRMED, CANCELLED, PENDING
    }
}
```

---

## Step 4: Repository Layer

### 4.1 Create User Repository
Create `src/main/java/com/ticketbooking/repository/UserRepository.java`:

```java
package com.ticketbooking.repository;

import com.ticketbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

### 4.2 Create Event Repository
Create `src/main/java/com/ticketbooking/repository/EventRepository.java`:

```java
package com.ticketbooking.repository;

import com.ticketbooking.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);
    
    @Query("SELECT e FROM Event e WHERE e.availableSeats > 0")
    List<Event> findAvailableEvents();
    
    @Query("SELECT e FROM Event e WHERE e.name LIKE %:name%")
    List<Event> findByNameContaining(@Param("name") String name);
}
```

### 4.3 Create Booking Repository
Create `src/main/java/com/ticketbooking/repository/BookingRepository.java`:

```java
package com.ticketbooking.repository;

import com.ticketbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByEventId(Long eventId);
    
    @Query("SELECT b FROM Booking b WHERE b.user.username = :username")
    List<Booking> findByUserUsername(@Param("username") String username);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.event.id = :eventId")
    Long countByEventId(@Param("eventId") Long eventId);
}
```

---

## Step 5: Service Layer

### 5.1 Create User Service
Create `src/main/java/com/ticketbooking/service/UserService.java`:

```java
package com.ticketbooking.service;

import com.ticketbooking.entity.User;
import com.ticketbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
```

### 5.2 Create Event Service
Create `src/main/java/com/ticketbooking/service/EventService.java`:

```java
package com.ticketbooking.service;

import com.ticketbooking.entity.Event;
import com.ticketbooking.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {
    
    @Autowired
    private EventRepository eventRepository;
    
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    
    public List<Event> getAvailableEvents() {
        return eventRepository.findAvailableEvents();
    }
    
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }
    
    public List<Event> searchEventsByName(String name) {
        return eventRepository.findByNameContaining(name);
    }
    
    public Event createEvent(Event event) {
        if (event.getAvailableSeats() == null) {
            event.setAvailableSeats(event.getTotalSeats());
        }
        return eventRepository.save(event);
    }
    
    public Event updateEvent(Event event) {
        return eventRepository.save(event);
    }
    
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }
    
    public Optional<Event> getEventByIdWithLock(Long id) {
        return eventRepository.findByIdWithLock(id);
    }
}
```

### 5.3 Create Booking Service with Multithreading
Create `src/main/java/com/ticketbooking/service/BookingService.java`:

```java
package com.ticketbooking.service;

import com.ticketbooking.entity.Booking;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.User;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BookingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Lock per event to handle concurrent bookings
    private final ConcurrentHashMap<Long, ReentrantLock> eventLocks = new ConcurrentHashMap<>();
    
    @Transactional
    public Booking bookTicket(Long userId, Long eventId, Integer seatsRequested) {
        logger.info("Booking request: User {} wants {} seats for event {}", userId, seatsRequested, eventId);
        
        // Get or create lock for this event
        ReentrantLock eventLock = eventLocks.computeIfAbsent(eventId, k -> new ReentrantLock(true));
        
        eventLock.lock();
        try {
            // Fetch user and event
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
            
            // Check availability
            if (event.getAvailableSeats() < seatsRequested) {
                throw new RuntimeException("Not enough seats available. Available: " + 
                    event.getAvailableSeats() + ", Requested: " + seatsRequested);
            }
            
            if (seatsRequested <= 0) {
                throw new RuntimeException("Number of seats must be positive");
            }
            
            // Update available seats
            event.setAvailableSeats(event.getAvailableSeats() - seatsRequested);
            eventRepository.save(event);
            
            // Create booking
            Double totalAmount = event.getPrice() * seatsRequested;
            Booking booking = new Booking(user, event, seatsRequested, totalAmount);
            
            Booking savedBooking = bookingRepository.save(booking);
            logger.info("Booking successful: {} for user {} with {} seats", 
                savedBooking.getId(), user.getUsername(), seatsRequested);
            
            return savedBooking;
            
        } finally {
            eventLock.unlock();
        }
    }
    
    @Async("bookingTaskExecutor")
    public CompletableFuture<Booking> bookTicketAsync(Long userId, Long eventId, Integer seatsRequested) {
        logger.info("Async booking request: User {} wants {} seats for event {}", userId, seatsRequested, eventId);
        
        try {
            Booking booking = bookTicket(userId, eventId, seatsRequested);
            return CompletableFuture.completedFuture(booking);
        } catch (Exception e) {
            logger.error("Async booking failed: {}", e.getMessage());
            CompletableFuture<Booking> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
    
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
    
    public List<Booking> getEventBookings(Long eventId) {
        return bookingRepository.findByEventId(eventId);
    }
    
    public List<Booking> getUserBookingsByUsername(String username) {
        return bookingRepository.findByUserUsername(username);
    }
    
    @Transactional
    public boolean cancelBooking(Long bookingId) {
        logger.info("Canceling booking: {}", bookingId);
        
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            
            if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
                throw new RuntimeException("Booking is already cancelled");
            }
            
            Event event = booking.getEvent();
            
            // Get lock for this event
            ReentrantLock eventLock = eventLocks.computeIfAbsent(event.getId(), k -> new ReentrantLock(true));
            eventLock.lock();
            
            try {
                // Return seats to available pool
                event.setAvailableSeats(event.getAvailableSeats() + booking.getSeatsBooked());
                eventRepository.save(event);
                
                // Update booking status
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                
                logger.info("Booking {} cancelled successfully", bookingId);
                return true;
                
            } finally {
                eventLock.unlock();
            }
        }
        
        logger.warn("Booking {} not found for cancellation", bookingId);
        return false;
    }
    
    public Long getBookingCountForEvent(Long eventId) {
        return bookingRepository.countByEventId(eventId);
    }
}
```

---

## Step 6: Configuration Classes

### 6.1 Create Thread Pool Configuration
Create `src/main/java/com/ticketbooking/config/ThreadPoolConfig.java`:

```java
package com.ticketbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {
    
    @Value("${ticket.booking.thread-pool.core-size:5}")
    private int corePoolSize;
    
    @Value("${ticket.booking.thread-pool.max-size:10}")
    private int maxPoolSize;
    
    @Value("${ticket.booking.thread-pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Bean(name = "bookingTaskExecutor")
    public Executor bookingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("BookingThread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
```

### 6.2 Create Security Configuration
Create `src/main/java/com/ticketbooking/config/SecurityConfig.java`:

```java
package com.ticketbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authz -> authz
                .antMatchers("/api/auth/**", "/h2-console/**", "/api/users/register").permitAll()
                .antMatchers("/api/events/**").hasAnyRole("USER", "ADMIN")
                .antMatchers("/api/bookings/**").hasAnyRole("USER", "ADMIN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic()
            .and()
            .headers().frameOptions().disable(); // For H2 console
        
        return http.build();
    }
}
```

---

## Step 7: Controller Layer

### 7.1 Create User Controller
Create `src/main/java/com/ticketbooking/controller/UserController.java`:

```java
package com.ticketbooking.controller;

import com.ticketbooking.entity.User;
import com.ticketbooking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
            .map(user -> ResponseEntity.ok(user))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User savedUser = userService.createUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }
}
```

### 7.2 Create Event Controller
Create `src/main/java/com/ticketbooking/controller/EventController.java`:

```java
package com.ticketbooking.controller;

import com.ticketbooking.entity.Event;
import com.ticketbooking.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    
    @Autowired
    private EventService eventService;
    
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<Event>> getAvailableEvents() {
        List<Event> events = eventService.getAvailableEvents();
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventService.getEventById(id)
            .map(event -> ResponseEntity.ok(event))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Event>> searchEvents(@RequestParam String name) {
        List<Event> events = eventService.searchEventsByName(name);
        return ResponseEntity.ok(events);
    }
    
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event savedEvent = eventService.createEvent(event);
        return ResponseEntity.ok(savedEvent);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        event.setId(id);
        Event updatedEvent = eventService.updateEvent(event);
        return ResponseEntity.ok(updatedEvent);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok("Event deleted successfully");
    }
}
```

### 7.3 Create Booking Controller
Create `src/main/java/com/ticketbooking/controller/BookingController.java`:

```java
package com.ticketbooking.controller;

import com.ticketbooking.entity.Booking;
import com.ticketbooking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    
    @Autowired
    private BookingService bookingService;
    
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id)
            .map(booking -> ResponseEntity.ok(booking))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/book")
    public ResponseEntity<?> bookTicket(@RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.bookTicket(
                request.getUserId(), 
                request.getEventId(), 
                request.getSeatsRequested()
            );
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/book-async")
    public CompletableFuture<ResponseEntity<?>> bookTicketAsync(@RequestBody BookingRequest request) {
        return bookingService.bookTicketAsync(
            request.getUserId(), 
            request.getEventId(), 
            request.getSeatsRequested()
        ).thenApply(booking -> ResponseEntity.ok(booking))
         .exceptionally(ex -> ResponseEntity.badRequest().body(ex.getCause().getMessage()));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable Long userId) {
        List<Booking> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Booking>> getEventBookings(@PathVariable Long eventId) {
        List<Booking> bookings = bookingService.getEventBookings(eventId);
        return ResponseEntity.ok(bookings);
    }
    
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId) {
        try {
            boolean cancelled = bookingService.cancelBooking(bookingId);
            if (cancelled) {
                return ResponseEntity.ok("Booking cancelled successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to cancel booking");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/count/event/{eventId}")
    public ResponseEntity<Long> getBookingCountForEvent(@PathVariable Long eventId) {
        Long count = bookingService.getBookingCountForEvent(eventId);
        return ResponseEntity.ok(count);
    }
    
    // Inner class for request body
    public static class BookingRequest {
        private Long userId;
        private Long eventId;
        private Integer seatsRequested;
        
        public BookingRequest() {}
        
        public BookingRequest(Long userId, Long eventId, Integer seatsRequested) {
            this.userId = userId;
            this.eventId = eventId;
            this.seatsRequested = seatsRequested;
        }
        
        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        
        public Integer getSeatsRequested() { return seatsRequested; }
        public void setSeatsRequested(Integer seatsRequested) { this.seatsRequested = seatsRequested; }
    }
}
```

---

## Step 8: Main Application Class

### 8.1 Create Main Application Class
Create `src/main/java/com/ticketbooking/TicketBookingSystemApplication.java`:

```java
package com.ticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class TicketBookingSystemApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TicketBookingSystemApplication.class, args);
    }
}
```

---

## Step 9: Data Initialization (Optional)

### 9.1 Create Data Loader
Create `src/main/java/com/ticketbooking/config/DataLoader.java`:

```java
package com.ticketbooking.config;

import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.User;
import com.ticketbooking.service.EventService;
import com.ticketbooking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataLoader implements ApplicationRunner {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EventService eventService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Create sample users
        if (!userService.existsByUsername("john_doe")) {
            User user1 = new User("john_doe", "password123", "john@example.com");
            userService.createUser(user1);
        }
        
        if (!userService.existsByUsername("jane_smith")) {
            User user2 = new User("jane_smith", "password123", "jane@example.com");
            userService.createUser(user2);
        }
        
        if (!userService.existsByUsername("admin_user")) {
            User admin = new User("admin_user", "admin123", "admin@example.com");
            admin.setRole("ADMIN");
            userService.createUser(admin);
        }
        
        // Create sample events
        if (eventService.getAllEvents().isEmpty()) {
            Event event1 = new Event(
                "Rock Concert", 
                "Stadium Arena", 
                LocalDateTime.now().plusDays(30), 
                100, 
                75.0
            );
            eventService.createEvent(event1);
            
            Event event2 = new Event(
                "Jazz Festival", 
                "Music Hall", 
                LocalDateTime.now().plusDays(45), 
                50, 
                50.0
            );
            eventService.createEvent(event2);
            
            Event event3 = new Event(
                "Comedy Show", 
                "Comedy Club", 
                LocalDateTime.now().plusDays(15), 
                25, 
                35.0
            );
            eventService.createEvent(event3);
        }
    }
}
```

---

## Step 10: Testing

### 10.1 Create Concurrent Booking Test
Create `src/test/java/com/ticketbooking/ConcurrentBookingTest.java`:

```java
package com.ticketbooking;

import com.ticketbooking.entity.Booking;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.User;
import com.ticketbooking.service.BookingService;
import com.ticketbooking.service.EventService;
import com.ticketbooking.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentBookingTest {
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private UserService userService;
    
    @Test
    public void testConcurrentBooking() throws Exception {
        // Create test event with limited seats
        Event event = new Event("Test Concert", "Test Venue", LocalDateTime.now().plusDays(1), 10, 100.0);
        Event savedEvent = eventService.createEvent(event);
        
        // Create test users
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            User user = new User("testuser" + i, "password", "testuser" + i + "@test.com");
            users.add(userService.createUser(user));
        }
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Simulate concurrent booking attempts
        for (User user : users) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Booking booking = bookingService.bookTicket(user.getId(), savedEvent.getId(), 1);
                    successCount.incrementAndGet();
                    System.out.println("✓ User " + user.getUsername() + " successfully booked ticket: " + booking.getId());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("✗ User " + user.getUsername() + " failed to book: " + e.getMessage());
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Check results
        Event updatedEvent = eventService.getEventById(savedEvent.getId()).orElse(null);
        System.out.println("\n=== Test Results ===");
        System.out.println("Initial seats: " + savedEvent.getTotalSeats());
        System.out.println("Final available seats: " + updatedEvent.getAvailableSeats());
        System.out.println("Successful bookings: " + successCount.get());
        System.out.println("Failed bookings: " + failureCount.get());
        System.out.println("Total booking attempts: " + users.size());
        
        // Verify no overselling occurred
        assert updatedEvent.getAvailableSeats() == (savedEvent.getTotalSeats() - successCount.get());
        assert successCount.get() <= savedEvent.getTotalSeats();
        
        executor.shutdown();
    }
}
```

### 10.2 Create test application.properties
Create `src/test/resources/application-test.properties`:

```properties
# Test Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# JPA Configuration for Testing
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Disable security for tests
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

# Logging for tests
logging.level.com.ticketbooking=INFO
```

---

## Step 11: Build and Run

### 11.1 Build the Project
```bash
mvn clean compile
mvn clean package
```

### 11.2 Run the Application
```bash
mvn spring-boot:run
```

### 11.3 Alternative: Run JAR
```bash
java -jar target/ticket-booking-system-1.0.0.jar
```

---

## Step 12: API Testing

### 12.1 Access H2 Database Console
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:ticketdb`
- Username: `sa`
- Password: `password`

### 12.2 Test API Endpoints

#### Register a New User
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "testuser@example.com"
  }'
```

#### Get All Events
```bash
curl -u admin:admin123 http://localhost:8080/api/events
```

#### Book a Ticket (Synchronous)
```bash
curl -X POST http://localhost:8080/api/bookings/book \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "eventId": 1,
    "seatsRequested": 2
  }'
```

#### Book a Ticket (Asynchronous)
```bash
curl -X POST http://localhost:8080/api/bookings/book-async \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "eventId": 1,
    "seatsRequested": 1
  }'
```

#### Get User Bookings
```bash
curl -u admin:admin123 http://localhost:8080/api/bookings/user/1
```

#### Cancel a Booking
```bash
curl -X DELETE http://localhost:8080/api/bookings/1 \
  -u admin:admin123
```

---

## Step 13: Project Structure

Your final project structure should look like this:

```
ticket-booking-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ticketbooking/
│   │   │           ├── TicketBookingSystemApplication.java
│   │   │           ├── config/
│   │   │           │   ├── DataLoader.java
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   └── ThreadPoolConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── BookingController.java
│   │   │           │   ├── EventController.java
│   │   │           │   └── UserController.java
│   │   │           ├── entity/
│   │   │           │   ├── Booking.java
│   │   │           │   ├── Event.java
│   │   │           │   └── User.java
│   │   │           ├── repository/
│   │   │           │   ├── BookingRepository.java
│   │   │           │   ├── EventRepository.java
│   │   │           │   └── UserRepository.java
│   │   │           └── service/
│   │   │               ├── BookingService.java
│   │   │               ├── EventService.java
│   │   │               └── UserService.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── ticketbooking/
│       │           └── ConcurrentBookingTest.java
│       └── resources/
│           └── application-test.properties
├── pom.xml
└── README.md
```

---

## Key Multithreading Features

### 1. **Pessimistic Locking**
- Uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` in EventRepository
- Prevents concurrent modifications to the same event

### 2. **ReentrantLock per Event**
- Each event has its own lock to prevent race conditions
- Allows concurrent bookings for different events

### 3. **Thread Pool Configuration**
- Custom thread pool for async operations
- Configurable core size, max size, and queue capacity

### 4. **Asynchronous Operations**
- `@Async` annotation for non-blocking booking operations
- CompletableFuture for async responses

### 5. **Optimistic Locking**
- `@Version` annotation in Event entity
- Handles concurrent updates gracefully

### 6. **Transaction Management**
- `@Transactional` ensures data consistency
- Automatic rollback on exceptions

---

## Troubleshooting

### Common Issues and Solutions

1. **Port Already in Use**
    - Change port in `application.properties`: `server.port=8081`

2. **Database Connection Issues**
    - Check H2 console at `http://localhost:8080/h2-console`
    - Verify JDBC URL: `jdbc:h2:mem:ticketdb`

3. **Authentication Errors**
    - Use basic auth with username/password from `application.properties`
    - Default: `admin:admin123`

4. **Concurrent Booking Test Failures**
    - Increase thread pool size in `application.properties`
    - Check logs for lock timeout issues

5. **Maven Build Errors**
    - Ensure Java 8+ is installed
    - Run `mvn clean install` to resolve dependencies

---

## Next Steps

1. **Add Frontend**: Create a React or Angular frontend
2. **Database Migration**: Switch from H2 to PostgreSQL/MySQL
3. **Email Notifications**: Send booking confirmations
4. **Payment Integration**: Add payment gateway
5. **Monitoring**: Add metrics and health checks
6. **Docker**: Containerize the application
7. **Testing**: Add more comprehensive tests

---

## Conclusion

This ticket booking system demonstrates:
- Thread-safe concurrent booking operations
- Proper database locking mechanisms
- RESTful API design
- Spring Boot best practices
- Security implementation
- Async programming with CompletableFuture

The system ensures no overselling occurs even under high concurrent load while maintaining good performance through proper thread management and database optimization.