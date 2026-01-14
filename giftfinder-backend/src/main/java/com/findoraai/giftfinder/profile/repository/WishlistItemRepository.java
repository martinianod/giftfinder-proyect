package com.findoraai.giftfinder.profile.repository;

import com.findoraai.giftfinder.profile.model.RecipientProfile;
import com.findoraai.giftfinder.profile.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByProfileOrderByPriorityDescCreatedAtDesc(RecipientProfile profile);
    Optional<WishlistItem> findByIdAndProfile(Long id, RecipientProfile profile);
}
