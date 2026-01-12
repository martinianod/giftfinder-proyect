package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.SavedProductRequest;
import com.findoraai.giftfinder.notifications.dto.SavedProductResponse;
import com.findoraai.giftfinder.notifications.model.PriceHistory;
import com.findoraai.giftfinder.notifications.model.Recipient;
import com.findoraai.giftfinder.notifications.model.SavedProduct;
import com.findoraai.giftfinder.notifications.repository.PriceHistoryRepository;
import com.findoraai.giftfinder.notifications.repository.RecipientRepository;
import com.findoraai.giftfinder.notifications.repository.SavedProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedProductServiceImpl implements SavedProductService {

    private final SavedProductRepository savedProductRepository;
    private final RecipientRepository recipientRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SavedProductResponse> getUserProducts(User user) {
        return savedProductRepository.findByUser(user).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SavedProductResponse getProduct(Long id, User user) {
        SavedProduct product = savedProductRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Saved product not found"));
        
        if (!product.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Saved product does not belong to user");
        }
        
        return toResponse(product);
    }

    @Override
    @Transactional
    public SavedProductResponse saveProduct(SavedProductRequest request, User user) {
        Recipient recipient = null;
        if (request.recipientId() != null) {
            recipient = recipientRepository.findById(request.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
            
            if (!recipient.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Recipient does not belong to user");
            }
        }
        
        SavedProduct product = SavedProduct.builder()
            .user(user)
            .recipient(recipient)
            .productId(request.productId())
            .title(request.title())
            .currentPrice(request.currentPrice())
            .currency(request.currency())
            .productUrl(request.productUrl())
            .imageUrl(request.imageUrl())
            .store(request.store())
            .priceTrackingEnabled(request.priceTrackingEnabled() != null ? request.priceTrackingEnabled() : true)
            .priceDropThresholdPercent(request.priceDropThresholdPercent() != null ? request.priceDropThresholdPercent() : new BigDecimal("10.00"))
            .build();
        
        product = savedProductRepository.save(product);
        
        // Create initial price history entry
        PriceHistory history = PriceHistory.builder()
            .savedProduct(product)
            .price(request.currentPrice())
            .currency(request.currency())
            .available(true)
            .build();
        priceHistoryRepository.save(history);
        
        return toResponse(product);
    }

    @Override
    @Transactional
    public SavedProductResponse updateProduct(Long id, SavedProductRequest request, User user) {
        SavedProduct product = savedProductRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Saved product not found"));
        
        if (!product.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Saved product does not belong to user");
        }
        
        Recipient recipient = null;
        if (request.recipientId() != null) {
            recipient = recipientRepository.findById(request.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
            
            if (!recipient.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Recipient does not belong to user");
            }
        }
        
        product.setRecipient(recipient);
        product.setTitle(request.title());
        product.setCurrentPrice(request.currentPrice());
        product.setCurrency(request.currency());
        product.setProductUrl(request.productUrl());
        product.setImageUrl(request.imageUrl());
        product.setStore(request.store());
        product.setPriceTrackingEnabled(request.priceTrackingEnabled() != null ? request.priceTrackingEnabled() : true);
        product.setPriceDropThresholdPercent(request.priceDropThresholdPercent() != null ? request.priceDropThresholdPercent() : new BigDecimal("10.00"));
        
        product = savedProductRepository.save(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, User user) {
        SavedProduct product = savedProductRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Saved product not found"));
        
        if (!product.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Saved product does not belong to user");
        }
        
        savedProductRepository.delete(product);
    }

    private SavedProductResponse toResponse(SavedProduct product) {
        return new SavedProductResponse(
            product.getId(),
            product.getProductId(),
            product.getTitle(),
            product.getCurrentPrice(),
            product.getCurrency(),
            product.getProductUrl(),
            product.getImageUrl(),
            product.getStore(),
            product.getRecipient() != null ? product.getRecipient().getId() : null,
            product.getRecipient() != null ? product.getRecipient().getName() : null,
            product.getPriceTrackingEnabled(),
            product.getPriceDropThresholdPercent(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
