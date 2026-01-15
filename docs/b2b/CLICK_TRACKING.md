# Click Tracking and Monetization

This document describes the outbound click tracking system for indirect monetization through affiliate links.

## Overview

The click tracking system records when users click on product links, enabling:
- Affiliate commission tracking
- Campaign attribution
- User behavior analytics
- A/B testing of different product sources

## Architecture

```
User clicks product → Create click record → Get redirect URL → Track click → Redirect to vendor
```

## Endpoints

### Create Click Tracking Link

Generate a trackable click link for a product.

**Request:**
```http
POST /api/clicks
Content-Type: application/json

{
  "productId": "PROD-12345",
  "provider": "mercadolibre",
  "targetUrl": "https://mercadolibre.com.ar/product/12345",
  "anonymousId": "anon-uuid-123",
  "campaignId": "holiday-2026",
  "trackingTags": "source:homepage,position:1",
  "utmSource": "giftfinder",
  "utmMedium": "product-recommendation",
  "utmCampaign": "holiday-2026"
}
```

**Response:**
```json
{
  "clickId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "redirectUrl": "/api/r/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Redirect via Click ID

Redirect user to product page while recording the click event.

**Request:**
```http
GET /api/r/{clickId}
```

**Response:**
```http
HTTP/1.1 302 Found
Location: https://mercadolibre.com.ar/product/12345?utm_source=giftfinder&utm_medium=product-recommendation&utm_campaign=holiday-2026
```

### Get Click Analytics (Admin Only)

Retrieve aggregated click analytics by provider and date.

**Request:**
```http
GET /api/clicks/analytics?days=30
Authorization: Bearer <admin-jwt-token>
```

**Response:**
```json
{
  "clicksByProvider": {
    "mercadolibre": 1250,
    "amazon": 830,
    "walmart": 420
  },
  "clicksByDate": {
    "2026-01-15": 150,
    "2026-01-14": 175,
    "2026-01-13": 142
  },
  "totalClicks": 2500
}
```

## UTM Parameters

The system supports standard UTM parameters for campaign tracking:

- `utm_source`: Traffic source (e.g., "giftfinder")
- `utm_medium`: Marketing medium (e.g., "product-recommendation", "email")
- `utm_campaign`: Campaign identifier (e.g., "holiday-2026")
- `utm_content`: Content variation (e.g., "banner-a")
- `utm_term`: Search term (e.g., "tech-gifts")

These parameters are automatically appended to the target URL during redirect.

## Campaign Attribution

Use `campaignId` and `trackingTags` to track attribution:

```json
{
  "campaignId": "holiday-2026",
  "trackingTags": "source:homepage,position:1,test:variant-a"
}
```

This enables:
- A/B test analysis
- Position-based performance tracking
- Multi-touch attribution

## Anonymous vs Authenticated Users

- **Authenticated users**: `userId` is recorded from JWT token
- **Anonymous users**: Provide `anonymousId` (e.g., from browser fingerprint or session)

Both user types generate clicks, enabling funnel analysis and conversion tracking.

## Database Schema

```sql
CREATE TABLE outbound_clicks (
    id BIGSERIAL PRIMARY KEY,
    click_id VARCHAR(100) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id),
    anonymous_id VARCHAR(100),
    product_id VARCHAR(200) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    target_url VARCHAR(2000) NOT NULL,
    campaign_id VARCHAR(100),
    tracking_tags VARCHAR(500),
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(100),
    utm_content VARCHAR(500),
    utm_term VARCHAR(100),
    clicked_at TIMESTAMP NOT NULL,
    INDEX idx_outbound_click_user (user_id),
    INDEX idx_outbound_click_anon (anonymous_id),
    INDEX idx_outbound_click_product (product_id),
    INDEX idx_outbound_click_provider (provider),
    INDEX idx_outbound_click_timestamp (clicked_at)
);
```

## Integration with Product Display

When displaying products, generate tracking links:

```javascript
// Frontend example
async function generateTrackingLink(product) {
  const response = await fetch('/api/clicks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      productId: product.id,
      provider: product.store,
      targetUrl: product.productUrl,
      anonymousId: getAnonymousId(),
      utmSource: 'giftfinder',
      utmMedium: 'recommendation',
      utmCampaign: 'search-results'
    })
  });
  
  const { redirectUrl } = await response.json();
  return redirectUrl;
}
```

## Privacy Considerations

- Click data is retained for analytics purposes
- Personal data (user_id) is optional
- Anonymous tracking uses session identifiers only
- Users can opt out via privacy settings (future enhancement)

## Future Enhancements

- Click-through rate (CTR) calculation
- Conversion tracking (if purchase data becomes available)
- Geographic distribution of clicks
- Device/browser breakdown
- Revenue attribution (with affiliate network integration)
