# Affiliate Provider Integration Guide

This guide explains how to integrate affiliate networks with GiftFinder AI.

## Overview

The `AffiliateProvider` interface defines a contract for integrating with affiliate networks like ShareASale, CJ (Commission Junction), Amazon Associates, Rakuten, and Impact.

## Interface Definition

```java
public interface AffiliateProvider {
    String getProviderName();
    boolean isEnabled();
    Optional<AffiliateLink> generateAffiliateLink(String productUrl, String campaignId);
    List<AffiliateProduct> searchProducts(String query, int maxResults);
    Optional<Double> getCommissionRate(String productId);
}
```

## Implementation Steps

### 1. Create Provider Implementation

```java
@Component
public class ShareASaleProvider implements AffiliateProvider {
    
    @Value("${affiliate.shareasale.api-key}")
    private String apiKey;
    
    @Value("${affiliate.shareasale.affiliate-id}")
    private String affiliateId;
    
    @Value("${affiliate.shareasale.enabled:false}")
    private boolean enabled;
    
    @Override
    public String getProviderName() {
        return "shareasale";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled && apiKey != null && affiliateId != null;
    }
    
    @Override
    public Optional<AffiliateLink> generateAffiliateLink(String productUrl, String campaignId) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        
        // Build ShareASale affiliate link
        // Format: https://shareasale.com/r.cfm?b=MERCHANT&u=AFFILIATE&m=MERCHANT_ID&urllink=PRODUCT_URL&afftrack=CAMPAIGN
        String affiliateUrl = UriComponentsBuilder
            .fromUriString("https://shareasale.com/r.cfm")
            .queryParam("u", affiliateId)
            .queryParam("m", extractMerchantId(productUrl))
            .queryParam("urllink", productUrl)
            .queryParam("afftrack", campaignId)
            .build()
            .toUriString();
            
        return Optional.of(AffiliateLink.builder()
            .originalUrl(productUrl)
            .affiliateUrl(affiliateUrl)
            .providerName(getProviderName())
            .campaignId(campaignId)
            .trackingCode(affiliateId)
            .build());
    }
    
    @Override
    public List<AffiliateProduct> searchProducts(String query, int maxResults) {
        // Call ShareASale Product API
        // API docs: https://account.shareasale.com/a-api.cfm
        
        String apiUrl = UriComponentsBuilder
            .fromUriString("https://api.shareasale.com/w.cfm")
            .queryParam("affiliateId", affiliateId)
            .queryParam("token", generateToken())
            .queryParam("format", "json")
            .queryParam("keyword", query)
            .queryParam("resultsPerPage", maxResults)
            .build()
            .toUriString();
            
        // Make HTTP request, parse response, map to AffiliateProduct
        // Return list of products
    }
    
    @Override
    public Optional<Double> getCommissionRate(String productId) {
        // Look up commission rate from cached merchant data or API
        return Optional.of(7.5); // Example: 7.5%
    }
    
    private String generateToken() {
        // Generate HMAC-SHA256 token for API authentication
        // See ShareASale API docs
    }
    
    private String extractMerchantId(String productUrl) {
        // Extract merchant ID from product URL
    }
}
```

### 2. Add Configuration

Add provider configuration to `application.yml`:

```yaml
affiliate:
  shareasale:
    enabled: ${SHAREASALE_ENABLED:false}
    api-key: ${SHAREASALE_API_KEY:}
    affiliate-id: ${SHAREASALE_AFFILIATE_ID:}
  cj:
    enabled: ${CJ_ENABLED:false}
    api-key: ${CJ_API_KEY:}
    website-id: ${CJ_WEBSITE_ID:}
  amazon:
    enabled: ${AMAZON_ENABLED:false}
    access-key: ${AMAZON_ACCESS_KEY:}
    secret-key: ${AMAZON_SECRET_KEY:}
    associate-tag: ${AMAZON_ASSOCIATE_TAG:}
```

### 3. Create Provider Service

```java
@Service
public class AffiliateProviderService {
    
    private final List<AffiliateProvider> providers;
    
    public AffiliateProviderService(List<AffiliateProvider> providers) {
        this.providers = providers;
    }
    
    public Optional<AffiliateLink> findBestAffiliateLink(String productUrl, String campaignId) {
        return providers.stream()
            .filter(AffiliateProvider::isEnabled)
            .map(provider -> provider.generateAffiliateLink(productUrl, campaignId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }
    
    public List<AffiliateProduct> searchAllProviders(String query, int maxResults) {
        return providers.stream()
            .filter(AffiliateProvider::isEnabled)
            .flatMap(provider -> provider.searchProducts(query, maxResults).stream())
            .collect(Collectors.toList());
    }
}
```

### 4. Update SavedProduct with Affiliate Data

When saving a product, enrich it with affiliate data:

```java
public SavedProduct saveProductWithAffiliate(SavedProductRequest request, Long userId) {
    SavedProduct product = SavedProduct.builder()
        .productId(request.getProductId())
        .title(request.getTitle())
        .productUrl(request.getProductUrl())
        // ... other fields
        .build();
    
    // Try to get affiliate link
    Optional<AffiliateLink> affiliateLink = affiliateProviderService
        .findBestAffiliateLink(request.getProductUrl(), "organic");
    
    affiliateLink.ifPresent(link -> {
        product.setAffiliateUrl(link.getAffiliateUrl());
        product.setCampaignId(link.getCampaignId());
        product.setTrackingTags("provider:" + link.getProviderName());
    });
    
    return savedProductRepository.save(product);
}
```

## Popular Affiliate Networks

### ShareASale
- **API Docs**: https://account.shareasale.com/a-api.cfm
- **Commission**: 5-20% depending on merchant
- **Best for**: Wide variety of merchants

### CJ (Commission Junction)
- **API Docs**: https://developers.cj.com/
- **Commission**: 3-15% depending on merchant
- **Best for**: Large retailers, enterprise merchants

### Amazon Associates
- **API**: Product Advertising API 5.0
- **Commission**: 1-10% depending on category
- **Best for**: Physical products, books, electronics

### Rakuten Advertising
- **API Docs**: https://developers.rakutenadvertising.com/
- **Commission**: 2-15% depending on merchant
- **Best for**: Fashion, home goods

### Impact
- **API Docs**: https://developer.impact.com/
- **Commission**: Varies by program
- **Best for**: Performance marketing, SaaS products

## Authentication Methods

Different networks use different authentication:

- **ShareASale**: HMAC-SHA256 token
- **CJ**: OAuth 2.0 + API Key
- **Amazon**: AWS Signature V4
- **Rakuten**: OAuth 2.0
- **Impact**: API Key

## Testing

Create integration tests for each provider:

```java
@SpringBootTest
class ShareASaleProviderTest {
    
    @Autowired
    private ShareASaleProvider provider;
    
    @Test
    void testGenerateAffiliateLink() {
        String productUrl = "https://example.com/product/123";
        Optional<AffiliateLink> link = provider.generateAffiliateLink(productUrl, "test-campaign");
        
        assertTrue(link.isPresent());
        assertTrue(link.get().getAffiliateUrl().contains("shareasale.com"));
        assertEquals("test-campaign", link.get().getCampaignId());
    }
    
    @Test
    void testSearchProducts() {
        List<AffiliateProduct> products = provider.searchProducts("tech gifts", 10);
        
        assertNotNull(products);
        assertTrue(products.size() <= 10);
        products.forEach(p -> {
            assertNotNull(p.getTitle());
            assertNotNull(p.getAffiliateUrl());
            assertTrue(p.getCommissionRate() > 0);
        });
    }
}
```

## Rate Limiting

Implement rate limiting to avoid API quota issues:

```java
@Component
public class RateLimitedShareASaleProvider implements AffiliateProvider {
    
    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 requests/sec
    
    @Override
    public List<AffiliateProduct> searchProducts(String query, int maxResults) {
        rateLimiter.acquire();
        // ... make API call
    }
}
```

## Error Handling

Handle provider failures gracefully:

```java
public Optional<AffiliateLink> generateAffiliateLink(String productUrl, String campaignId) {
    try {
        // ... provider logic
    } catch (HttpClientErrorException e) {
        log.error("Provider API error: {}", e.getMessage());
        return Optional.empty();
    } catch (Exception e) {
        log.error("Unexpected error generating affiliate link", e);
        return Optional.empty();
    }
}
```

## Caching

Cache affiliate data to reduce API calls:

```java
@Cacheable(value = "affiliateLinks", key = "#productUrl")
public Optional<AffiliateLink> generateAffiliateLink(String productUrl, String campaignId) {
    // ... implementation
}
```

## Metrics and Monitoring

Track provider performance:

```java
@Timed(value = "affiliate.provider.search", percentiles = {0.5, 0.95, 0.99})
public List<AffiliateProduct> searchProducts(String query, int maxResults) {
    // ... implementation
}
```

Monitor:
- API response times
- Error rates by provider
- Click-through rates
- Commission earnings (if available via API)

## Next Steps

1. Sign up for affiliate network accounts
2. Get API credentials
3. Implement provider for your chosen network
4. Test in development environment
5. Monitor performance and optimize
6. Scale to additional networks
