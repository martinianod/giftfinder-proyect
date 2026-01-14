# Shareable Recipient Profiles API Documentation

## Overview

The Shareable Recipient Profiles feature allows users to create detailed gift recipient profiles that can be shared via a secure tokenized link. Anyone with the link can view the profile, wishlist, and get AI-powered gift recommendations.

## Features

- ✅ Create and manage recipient profiles with detailed preferences
- ✅ Toggle profile visibility: PRIVATE / SHARED_LINK
- ✅ Generate secure, unguessable share links
- ✅ Public endpoint for viewing shared profiles
- ✅ Wishlist management for each profile
- ✅ AI-powered gift recommendations based on profile data
- ✅ Rate limiting on public endpoints (30 requests/minute per IP)
- ✅ Token expiration (default: 1 year)
- ✅ Privacy protection (no user emails or sensitive data exposed)

## Authentication

Most endpoints require JWT authentication via Bearer token in the `Authorization` header:

```
Authorization: Bearer <your-jwt-token>
```

Public endpoints (`/public/**`) do NOT require authentication.

## Endpoints

### Profile Management (Authenticated)

#### Create Profile
```http
POST /api/profiles
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "John Doe",
  "relationship": "friend",
  "interests": "technology, gaming, photography",
  "restrictions": "no food items",
  "sizes": "M",
  "preferredStores": "Amazon, Best Buy",
  "budgetMin": 50.00,
  "budgetMax": 200.00,
  "visibility": "PRIVATE",
  "claimEmail": "john@example.com"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "John Doe",
  "relationship": "friend",
  "interests": "technology, gaming, photography",
  "restrictions": "no food items",
  "sizes": "M",
  "preferredStores": "Amazon, Best Buy",
  "budgetMin": 50.00,
  "budgetMax": 200.00,
  "visibility": "PRIVATE",
  "shareUrl": null,
  "claimed": false,
  "claimEmail": "john@example.com",
  "wishlistItems": [],
  "createdAt": "2024-01-14T20:00:00",
  "updatedAt": "2024-01-14T20:00:00"
}
```

#### List Profiles
```http
GET /api/profiles
Authorization: Bearer <token>
```

#### Get Profile
```http
GET /api/profiles/{id}
Authorization: Bearer <token>
```

#### Update Profile
```http
PUT /api/profiles/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "John Doe Updated",
  "relationship": "best friend",
  "interests": "technology, gaming, photography, cooking",
  "restrictions": "no food items",
  "sizes": "L",
  "preferredStores": "Amazon, Best Buy, Target",
  "budgetMin": 100.00,
  "budgetMax": 300.00,
  "visibility": "SHARED_LINK",
  "claimEmail": "john@example.com"
}
```

#### Delete Profile
```http
DELETE /api/profiles/{id}
Authorization: Bearer <token>
```

#### Generate Share Link
```http
POST /api/profiles/{id}/share
Authorization: Bearer <token>
```

**Response:**
```json
{
  "shareUrl": "http://localhost:5173/public/recipient/abc123xyz456"
}
```

### Wishlist Management (Authenticated)

#### Add Wishlist Item
```http
POST /api/profiles/{profileId}/wishlist
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Wireless Headphones",
  "description": "Noise-cancelling over-ear headphones",
  "url": "https://www.amazon.com/...",
  "price": 150.00,
  "currency": "ARS",
  "imageUrl": "https://...",
  "priority": 5,
  "purchased": false
}
```

#### List Wishlist Items
```http
GET /api/profiles/{profileId}/wishlist
Authorization: Bearer <token>
```

#### Get Wishlist Item
```http
GET /api/profiles/{profileId}/wishlist/{itemId}
Authorization: Bearer <token>
```

#### Update Wishlist Item
```http
PUT /api/profiles/{profileId}/wishlist/{itemId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Wireless Headphones (Updated)",
  "description": "Premium noise-cancelling over-ear headphones",
  "url": "https://www.amazon.com/...",
  "price": 180.00,
  "currency": "ARS",
  "imageUrl": "https://...",
  "priority": 8,
  "purchased": false
}
```

#### Delete Wishlist Item
```http
DELETE /api/profiles/{profileId}/wishlist/{itemId}
Authorization: Bearer <token>
```

### Public Endpoints (No Authentication)

#### View Public Profile
```http
GET /public/recipient/{token}
```

**Response:**
```json
{
  "name": "John Doe",
  "relationship": "friend",
  "interests": "technology, gaming, photography",
  "restrictions": "no food items",
  "sizes": "M",
  "preferredStores": "Amazon, Best Buy",
  "budgetMin": 50.00,
  "budgetMax": 200.00,
  "wishlistItems": [
    {
      "title": "Wireless Headphones",
      "description": "Noise-cancelling over-ear headphones",
      "url": "https://www.amazon.com/...",
      "price": 150.00,
      "currency": "ARS",
      "imageUrl": "https://...",
      "priority": 5,
      "purchased": false
    }
  ],
  "claimed": false
}
```

**Note:** User email and internal IDs are NOT exposed in public responses.

#### Get Gift Recommendations
```http
GET /public/recipient/{token}/recommendations
```

This endpoint generates AI-powered gift recommendations based on the profile's interests, relationship, and preferences using the scraper service.

**Response:**
```json
{
  "interpretedIntent": {
    "recipient": "friend",
    "age": null,
    "budgetMin": 50.0,
    "budgetMax": 200.0,
    "interests": ["technology", "gaming", "photography"]
  },
  "recommendations": [
    {
      "id": "uuid",
      "title": "Product Name",
      "price": 12345.0,
      "currency": "ARS",
      "image_url": "https://...",
      "product_url": "https://mercadolibre.com.ar/...",
      "store": "MercadoLibre",
      "tags": ["technology"]
    }
  ]
}
```

## Data Models

### RecipientProfile

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | Auto | Profile ID |
| name | String | Yes | Recipient name |
| relationship | String | No | Relationship to user (friend, family, etc.) |
| interests | String | No | Comma-separated interests (max 2000 chars) |
| restrictions | String | No | Gift restrictions (max 1000 chars) |
| sizes | String | No | Clothing/shoe sizes (max 500 chars) |
| preferredStores | String | No | Preferred stores (max 500 chars) |
| budgetMin | BigDecimal | No | Minimum budget |
| budgetMax | BigDecimal | No | Maximum budget |
| visibility | Enum | Yes | PRIVATE or SHARED_LINK |
| claimEmail | String | No | Email for profile claiming |
| claimed | Boolean | Auto | Whether profile has been claimed |

### WishlistItem

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | Auto | Item ID |
| title | String | Yes | Item title (max 500 chars) |
| description | String | No | Item description (max 2000 chars) |
| url | String | No | Product URL (max 1000 chars) |
| price | BigDecimal | No | Item price |
| currency | String | No | Currency code (default: ARS) |
| imageUrl | String | No | Image URL (max 1000 chars) |
| priority | Integer | No | Priority level (default: 0) |
| purchased | Boolean | No | Purchase status (default: false) |

### ShareLinkToken

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Token ID |
| hashedToken | String | Secure token for URL |
| createdAt | DateTime | Creation timestamp |
| expiresAt | DateTime | Expiration timestamp (default: +1 year) |

## Security

### Token Security

- Tokens are 32-byte (256-bit) random values generated using `SecureRandom`
- Tokens are encoded in Base64 URL-safe format (43 characters)
- Tokens are cryptographically secure and unguessable (2^256 possible values)
- Tokens are stored directly in database (not hashed) for URL lookups
- Security relies on high entropy (256 bits) making brute-force attacks infeasible
- Default expiration: 1 year from creation
- Expired tokens are rejected with 400 error

**Note on token storage:** While tokens are not cryptographically hashed, they are unguessable due to their 256-bit entropy. This provides excellent security while allowing direct database lookups for performance. BCrypt hashing would not add meaningful security since tokens are never provided by users but rather generated randomly.

### Rate Limiting

Public endpoints are rate-limited to **30 requests per minute per IP address**.

- Exceeding the limit returns HTTP 429 (Too Many Requests)
- Rate limit applies to all `/public/**` endpoints
- Uses X-Forwarded-For header to detect real IP behind proxies

### Privacy Protection

Public endpoints DO NOT expose:
- ❌ User email addresses
- ❌ User IDs
- ❌ Internal database IDs
- ❌ Profile owner information
- ❌ Sensitive metadata

Public endpoints ONLY expose:
- ✅ Recipient name and relationship
- ✅ Interests, restrictions, sizes, preferred stores
- ✅ Budget ranges
- ✅ Wishlist items
- ✅ Claimed status

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid request parameters",
  "message": "Name is required"
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Authentication required"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Profile not found"
}
```

### 429 Too Many Requests
```json
{
  "error": "Too many requests. Please try again later."
}
```

## Usage Examples

### Complete Workflow

1. **Create a profile:**
   ```bash
   curl -X POST http://localhost:8080/api/profiles \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{
       "name": "Sarah",
       "relationship": "sister",
       "interests": "books, coffee, travel",
       "visibility": "SHARED_LINK",
       "budgetMin": 30,
       "budgetMax": 100
     }'
   ```

2. **Get the share link:**
   ```bash
   curl -X POST http://localhost:8080/api/profiles/1/share \
     -H "Authorization: Bearer <token>"
   ```

3. **Share the link with others** (no authentication needed):
   ```
   http://localhost:5173/public/recipient/abc123xyz456
   ```

4. **Anyone with the link can view the profile:**
   ```bash
   curl http://localhost:8080/public/recipient/abc123xyz456
   ```

5. **Get AI-powered gift recommendations:**
   ```bash
   curl http://localhost:8080/public/recipient/abc123xyz456/recommendations
   ```

## Database Schema

The feature creates three new tables:

- `recipient_profiles` - Stores profile information
- `wishlist_items` - Stores wishlist items linked to profiles
- `share_link_tokens` - Stores secure share tokens

All tables are automatically created by JPA with `ddl-auto: update`.

## Configuration

No additional configuration required. The feature uses existing application settings:

- `app.base-url` - Base URL for share links (default: http://localhost:5173)
- `scraper.base-url` - Scraper service URL for recommendations

## Future Enhancements

- [ ] Email verification for profile claiming
- [ ] Allow recipients to manage their own wishlists after claiming
- [ ] Notification when someone views the shared profile
- [ ] Export profile as PDF
- [ ] Multiple share links with different permissions
- [ ] Analytics for shared profile views
