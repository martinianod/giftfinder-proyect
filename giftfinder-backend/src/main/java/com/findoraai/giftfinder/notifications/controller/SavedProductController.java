package com.findoraai.giftfinder.notifications.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.SavedProductRequest;
import com.findoraai.giftfinder.notifications.dto.SavedProductResponse;
import com.findoraai.giftfinder.notifications.service.SavedProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saved-products")
@RequiredArgsConstructor
public class SavedProductController {

    private final SavedProductService savedProductService;

    @GetMapping
    public ResponseEntity<List<SavedProductResponse>> getUserProducts(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        List<SavedProductResponse> products = savedProductService.getUserProducts(user);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedProductResponse> getProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        SavedProductResponse product = savedProductService.getProduct(id, user);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<SavedProductResponse> saveProduct(
            @Valid @RequestBody SavedProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        SavedProductResponse product = savedProductService.saveProduct(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavedProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody SavedProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        SavedProductResponse product = savedProductService.updateProduct(id, request, user);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        savedProductService.deleteProduct(id, user);
        return ResponseEntity.noContent().build();
    }
}
