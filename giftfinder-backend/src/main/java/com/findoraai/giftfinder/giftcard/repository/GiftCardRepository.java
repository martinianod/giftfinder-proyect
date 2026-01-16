package com.findoraai.giftfinder.giftcard.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.model.GiftCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GiftCardRepository extends JpaRepository<GiftCard, Long> {
    Optional<GiftCard> findByCode(String code);
    List<GiftCard> findBySenderOrderByCreatedAtDesc(User sender);
    List<GiftCard> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
    Optional<GiftCard> findByHashedCode(String hashedCode);
}
