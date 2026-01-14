package com.findoraai.giftfinder.profile.repository;

import com.findoraai.giftfinder.profile.model.RecipientProfile;
import com.findoraai.giftfinder.profile.model.ShareLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareLinkTokenRepository extends JpaRepository<ShareLinkToken, Long> {
    Optional<ShareLinkToken> findByHashedToken(String hashedToken);
    Optional<ShareLinkToken> findByProfile(RecipientProfile profile);
}
