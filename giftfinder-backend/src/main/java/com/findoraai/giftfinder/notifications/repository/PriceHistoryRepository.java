package com.findoraai.giftfinder.notifications.repository;

import com.findoraai.giftfinder.notifications.model.PriceHistory;
import com.findoraai.giftfinder.notifications.model.SavedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findBySavedProductOrderByCheckedAtDesc(SavedProduct savedProduct);
    
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.savedProduct = :product ORDER BY ph.checkedAt DESC LIMIT 1")
    Optional<PriceHistory> findLatestByProduct(@Param("product") SavedProduct product);
}
