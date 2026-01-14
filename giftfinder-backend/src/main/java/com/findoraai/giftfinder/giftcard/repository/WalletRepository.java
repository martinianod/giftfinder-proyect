package com.findoraai.giftfinder.giftcard.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser(User user);
}
