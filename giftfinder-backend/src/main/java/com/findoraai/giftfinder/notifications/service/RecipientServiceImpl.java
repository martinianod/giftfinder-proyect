package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.RecipientRequest;
import com.findoraai.giftfinder.notifications.dto.RecipientResponse;
import com.findoraai.giftfinder.notifications.model.Recipient;
import com.findoraai.giftfinder.notifications.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipientServiceImpl implements RecipientService {

    private final RecipientRepository recipientRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecipientResponse> getUserRecipients(User user) {
        return recipientRepository.findByUserOrderByNameAsc(user).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecipientResponse getRecipient(Long id, User user) {
        Recipient recipient = recipientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        
        if (!recipient.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Recipient does not belong to user");
        }
        
        return toResponse(recipient);
    }

    @Override
    @Transactional
    public RecipientResponse createRecipient(RecipientRequest request, User user) {
        Recipient recipient = Recipient.builder()
            .user(user)
            .name(request.name())
            .description(request.description())
            .birthday(request.birthday())
            .build();
        
        recipient = recipientRepository.save(recipient);
        return toResponse(recipient);
    }

    @Override
    @Transactional
    public RecipientResponse updateRecipient(Long id, RecipientRequest request, User user) {
        Recipient recipient = recipientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        
        if (!recipient.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Recipient does not belong to user");
        }
        
        recipient.setName(request.name());
        recipient.setDescription(request.description());
        recipient.setBirthday(request.birthday());
        
        recipient = recipientRepository.save(recipient);
        return toResponse(recipient);
    }

    @Override
    @Transactional
    public void deleteRecipient(Long id, User user) {
        Recipient recipient = recipientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
        
        if (!recipient.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Recipient does not belong to user");
        }
        
        recipientRepository.delete(recipient);
    }

    private RecipientResponse toResponse(Recipient recipient) {
        return new RecipientResponse(
            recipient.getId(),
            recipient.getName(),
            recipient.getDescription(),
            recipient.getBirthday(),
            recipient.getCreatedAt(),
            recipient.getUpdatedAt()
        );
    }
}
