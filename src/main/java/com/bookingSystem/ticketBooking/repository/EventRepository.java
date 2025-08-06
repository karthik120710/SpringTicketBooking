package com.bookingSystem.ticketBooking.repository;

import com.bookingSystem.ticketBooking.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
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