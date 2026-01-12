package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.SavedProductRequest;
import com.findoraai.giftfinder.notifications.dto.SavedProductResponse;

import java.util.List;

public interface SavedProductService {
    List<SavedProductResponse> getUserProducts(User user);
    SavedProductResponse getProduct(Long id, User user);
    SavedProductResponse saveProduct(SavedProductRequest request, User user);
    SavedProductResponse updateProduct(Long id, SavedProductRequest request, User user);
    void deleteProduct(Long id, User user);
}
