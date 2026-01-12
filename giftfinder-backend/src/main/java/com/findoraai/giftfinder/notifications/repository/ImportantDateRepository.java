package com.findoraai.giftfinder.notifications.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.model.ImportantDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ImportantDateRepository extends JpaRepository<ImportantDate, Long> {
    List<ImportantDate> findByUser(User user);
    
    @Query("SELECT d FROM ImportantDate d WHERE d.user = :user AND FUNCTION('MONTH', d.date) = FUNCTION('MONTH', :date) AND FUNCTION('DAY', d.date) = FUNCTION('DAY', :date)")
    List<ImportantDate> findUpcomingDates(@Param("user") User user, @Param("date") LocalDate date);
    
    @Query("SELECT d FROM ImportantDate d WHERE d.date BETWEEN :startDate AND :endDate")
    List<ImportantDate> findDatesBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
