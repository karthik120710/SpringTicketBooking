package com.bookingSystem.ticketBooking.controller;

import com.bookingSystem.ticketBooking.entity.Booking;
import com.bookingSystem.ticketBooking.service.BookingService;
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
        ).handle((booking, ex) -> {
            if (ex != null) {
                String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                return ResponseEntity.badRequest().body(errorMessage);
            } else {
                return ResponseEntity.ok(booking);
            }
        });
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
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        
        public Integer getSeatsRequested() { return seatsRequested; }
        public void setSeatsRequested(Integer seatsRequested) { this.seatsRequested = seatsRequested; }
    }
}