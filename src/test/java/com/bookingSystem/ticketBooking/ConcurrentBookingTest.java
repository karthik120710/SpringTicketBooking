package com.bookingSystem.ticketBooking;

import com.bookingSystem.ticketBooking.entity.Booking;
import com.bookingSystem.ticketBooking.entity.Event;
import com.bookingSystem.ticketBooking.entity.User;
import com.bookingSystem.ticketBooking.service.BookingService;
import com.bookingSystem.ticketBooking.service.EventService;
import com.bookingSystem.ticketBooking.service.UserService;
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
        Event event = new Event("Test Concert", "Test Venue", LocalDateTime.now().plusDays(1), 10, 100.0);
        Event savedEvent = eventService.createEvent(event);
        
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            User user = new User("testuser" + i, "password", "testuser" + i + "@test.com");
            users.add(userService.createUser(user));
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
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
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        Event updatedEvent = eventService.getEventById(savedEvent.getId()).orElse(null);
        System.out.println("\n=== Test Results ===");
        System.out.println("Initial seats: " + savedEvent.getTotalSeats());
        System.out.println("Final available seats: " + updatedEvent.getAvailableSeats());
        System.out.println("Successful bookings: " + successCount.get());
        System.out.println("Failed bookings: " + failureCount.get());
        System.out.println("Total booking attempts: " + users.size());
        
        assert updatedEvent.getAvailableSeats() == (savedEvent.getTotalSeats() - successCount.get());
        assert successCount.get() <= savedEvent.getTotalSeats();
        
        executor.shutdown();
    }
}