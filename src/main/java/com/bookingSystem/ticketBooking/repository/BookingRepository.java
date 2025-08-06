package com.bookingSystem.ticketBooking.repository;

import com.bookingSystem.ticketBooking.entity.Booking;
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