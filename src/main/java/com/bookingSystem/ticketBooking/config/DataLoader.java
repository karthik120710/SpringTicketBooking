package com.bookingSystem.ticketBooking.config;

import com.bookingSystem.ticketBooking.entity.Event;
import com.bookingSystem.ticketBooking.entity.User;
import com.bookingSystem.ticketBooking.service.EventService;
import com.bookingSystem.ticketBooking.service.UserService;
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