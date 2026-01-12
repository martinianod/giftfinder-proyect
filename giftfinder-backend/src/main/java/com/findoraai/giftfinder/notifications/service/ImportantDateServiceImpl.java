package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.ImportantDateRequest;
import com.findoraai.giftfinder.notifications.dto.ImportantDateResponse;
import com.findoraai.giftfinder.notifications.model.ImportantDate;
import com.findoraai.giftfinder.notifications.model.Recipient;
import com.findoraai.giftfinder.notifications.repository.ImportantDateRepository;
import com.findoraai.giftfinder.notifications.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportantDateServiceImpl implements ImportantDateService {

    private final ImportantDateRepository importantDateRepository;
    private final RecipientRepository recipientRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ImportantDateResponse> getUserDates(User user) {
        return importantDateRepository.findByUser(user).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ImportantDateResponse getDate(Long id, User user) {
        ImportantDate date = importantDateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Important date not found"));
        
        if (!date.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Important date does not belong to user");
        }
        
        return toResponse(date);
    }

    @Override
    @Transactional
    public ImportantDateResponse createDate(ImportantDateRequest request, User user) {
        Recipient recipient = null;
        if (request.recipientId() != null) {
            recipient = recipientRepository.findById(request.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
            
            if (!recipient.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Recipient does not belong to user");
            }
        }
        
        ImportantDate date = ImportantDate.builder()
            .user(user)
            .recipient(recipient)
            .name(request.name())
            .type(request.type())
            .date(request.date())
            .recurring(request.recurring() != null ? request.recurring() : false)
            .description(request.description())
            .build();
        
        date = importantDateRepository.save(date);
        return toResponse(date);
    }

    @Override
    @Transactional
    public ImportantDateResponse updateDate(Long id, ImportantDateRequest request, User user) {
        ImportantDate date = importantDateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Important date not found"));
        
        if (!date.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Important date does not belong to user");
        }
        
        Recipient recipient = null;
        if (request.recipientId() != null) {
            recipient = recipientRepository.findById(request.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
            
            if (!recipient.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Recipient does not belong to user");
            }
        }
        
        date.setRecipient(recipient);
        date.setName(request.name());
        date.setType(request.type());
        date.setDate(request.date());
        date.setRecurring(request.recurring() != null ? request.recurring() : false);
        date.setDescription(request.description());
        
        date = importantDateRepository.save(date);
        return toResponse(date);
    }

    @Override
    @Transactional
    public void deleteDate(Long id, User user) {
        ImportantDate date = importantDateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Important date not found"));
        
        if (!date.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Important date does not belong to user");
        }
        
        importantDateRepository.delete(date);
    }

    private ImportantDateResponse toResponse(ImportantDate date) {
        return new ImportantDateResponse(
            date.getId(),
            date.getName(),
            date.getType(),
            date.getDate(),
            date.getRecurring(),
            date.getDescription(),
            date.getRecipient() != null ? date.getRecipient().getId() : null,
            date.getRecipient() != null ? date.getRecipient().getName() : null,
            date.getCreatedAt(),
            date.getUpdatedAt()
        );
    }
}
