package com.findoraai.giftfinder.notifications.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.model.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    List<Recipient> findByUser(User user);
    List<Recipient> findByUserOrderByNameAsc(User user);
}
