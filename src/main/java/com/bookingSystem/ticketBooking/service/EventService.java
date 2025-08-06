package com.bookingSystem.ticketBooking.service;

import com.bookingSystem.ticketBooking.entity.Event;
import com.bookingSystem.ticketBooking.repository.EventRepository;
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