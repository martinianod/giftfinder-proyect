package com.findoraai.giftfinder.notifications.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.model.SavedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedProductRepository extends JpaRepository<SavedProduct, Long> {
    List<SavedProduct> findByUser(User user);
    
    List<SavedProduct> findByUserAndPriceTrackingEnabled(User user, Boolean priceTrackingEnabled);
    
    List<SavedProduct> findByPriceTrackingEnabled(Boolean priceTrackingEnabled);
    
    Optional<SavedProduct> findByUserAndProductId(User user, String productId);
}
