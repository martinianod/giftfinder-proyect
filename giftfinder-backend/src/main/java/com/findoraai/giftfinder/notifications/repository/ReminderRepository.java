package com.findoraai.giftfinder.notifications.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.model.ImportantDate;
import com.findoraai.giftfinder.notifications.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUser(User user);
    
    List<Reminder> findByStatusAndScheduledDate(Reminder.ReminderStatus status, LocalDate scheduledDate);
    
    @Query("SELECT r FROM Reminder r WHERE r.scheduledDate <= :date AND r.status = :status")
    List<Reminder> findDueReminders(@Param("date") LocalDate date, @Param("status") Reminder.ReminderStatus status);
    
    @Query("SELECT r FROM Reminder r WHERE r.user = :user AND r.importantDate = :importantDate AND r.daysBefore = :daysBefore AND r.scheduledDate = :scheduledDate")
    Optional<Reminder> findByUserAndImportantDateAndDaysBeforeAndScheduledDate(
        @Param("user") User user, 
        @Param("importantDate") ImportantDate importantDate, 
        @Param("daysBefore") Integer daysBefore, 
        @Param("scheduledDate") LocalDate scheduledDate);
    
    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.user = :user AND r.importantDate = :importantDate AND r.daysBefore = :daysBefore AND r.scheduledDate = :scheduledDate")
    long countByUserAndImportantDateAndDaysBeforeAndScheduledDate(
        @Param("user") User user, 
        @Param("importantDate") ImportantDate importantDate, 
        @Param("daysBefore") Integer daysBefore, 
        @Param("scheduledDate") LocalDate scheduledDate);
    
    default boolean existsByUserAndImportantDateAndDaysBeforeAndScheduledDate(
            User user, ImportantDate importantDate, Integer daysBefore, LocalDate scheduledDate) {
        return countByUserAndImportantDateAndDaysBeforeAndScheduledDate(user, importantDate, daysBefore, scheduledDate) > 0;
    }
}
