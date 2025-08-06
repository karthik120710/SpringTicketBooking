package com.bookingSystem.ticketBooking.service;

import com.bookingSystem.ticketBooking.entity.Booking;
import com.bookingSystem.ticketBooking.entity.Event;
import com.bookingSystem.ticketBooking.entity.User;
import com.bookingSystem.ticketBooking.repository.BookingRepository;
import com.bookingSystem.ticketBooking.repository.EventRepository;
import com.bookingSystem.ticketBooking.repository.UserRepository;
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
    
    private final ConcurrentHashMap<Long, ReentrantLock> eventLocks = new ConcurrentHashMap<>();
    
    @Transactional
    public Booking bookTicket(Long userId, Long eventId, Integer seatsRequested) {
        logger.info("Booking request: User {} wants {} seats for event {}", userId, seatsRequested, eventId);
        
        ReentrantLock eventLock = eventLocks.computeIfAbsent(eventId, k -> new ReentrantLock(true));
        
        eventLock.lock();
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
            
            if (event.getAvailableSeats() < seatsRequested) {
                throw new RuntimeException("Not enough seats available. Available: " + 
                    event.getAvailableSeats() + ", Requested: " + seatsRequested);
            }
            
            if (seatsRequested <= 0) {
                throw new RuntimeException("Number of seats must be positive");
            }
            
            event.setAvailableSeats(event.getAvailableSeats() - seatsRequested);
            eventRepository.save(event);
            
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
            
            ReentrantLock eventLock = eventLocks.computeIfAbsent(event.getId(), k -> new ReentrantLock(true));
            eventLock.lock();
            
            try {
                event.setAvailableSeats(event.getAvailableSeats() + booking.getSeatsBooked());
                eventRepository.save(event);
                
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