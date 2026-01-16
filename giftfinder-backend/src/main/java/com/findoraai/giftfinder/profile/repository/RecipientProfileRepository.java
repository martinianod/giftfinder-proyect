package com.findoraai.giftfinder.profile.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.model.RecipientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipientProfileRepository extends JpaRepository<RecipientProfile, Long> {
    List<RecipientProfile> findByUserOrderByCreatedAtDesc(User user);
    Optional<RecipientProfile> findByIdAndUser(Long id, User user);
    Optional<RecipientProfile> findByClaimEmail(String claimEmail);
}
