package com.findoraai.giftfinder.tracking.repository;

import com.findoraai.giftfinder.tracking.model.OutboundClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboundClickRepository extends JpaRepository<OutboundClick, Long> {
    Optional<OutboundClick> findByClickId(String clickId);
    
    @Query("SELECT c.provider, COUNT(c) FROM OutboundClick c WHERE c.clickedAt >= :startDate GROUP BY c.provider")
    List<Object[]> countClicksByProvider(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT DATE(c.clickedAt), COUNT(c) FROM OutboundClick c WHERE c.clickedAt >= :startDate GROUP BY DATE(c.clickedAt) ORDER BY DATE(c.clickedAt)")
    List<Object[]> countClicksByDate(@Param("startDate") LocalDateTime startDate);
}
