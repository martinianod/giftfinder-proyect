package com.findoraai.giftfinder.affiliate.provider;

import com.findoraai.giftfinder.affiliate.dto.AffiliateLink;
import com.findoraai.giftfinder.affiliate.dto.AffiliateProduct;

import java.util.List;
import java.util.Optional;

/**
 * Contract for affiliate provider integrations.
 * Implementations can integrate with various affiliate networks 
 * (e.g., ShareASale, CJ, Amazon Associates, Rakuten, Impact).
 */
public interface AffiliateProvider {
    
    /**
     * Get the provider name (e.g., "shareasale", "cj", "amazon")
     */
    String getProviderName();
    
    /**
     * Check if this provider is enabled and configured
     */
    boolean isEnabled();
    
    /**
     * Generate an affiliate link for a given product URL
     * 
     * @param productUrl Original product URL
     * @param campaignId Optional campaign identifier for tracking
     * @return AffiliateLink with tracking URL and metadata
     */
    Optional<AffiliateLink> generateAffiliateLink(String productUrl, String campaignId);
    
    /**
     * Search for products via the affiliate network API
     * 
     * @param query Search query
     * @param maxResults Maximum number of results
     * @return List of affiliate products with commission info
     */
    List<AffiliateProduct> searchProducts(String query, int maxResults);
    
    /**
     * Get commission rate for a specific product or category
     * 
     * @param productId Provider-specific product identifier
     * @return Commission rate as percentage (e.g., 5.0 for 5%)
     */
    Optional<Double> getCommissionRate(String productId);
}
