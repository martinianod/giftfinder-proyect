# B2B Organizations API

This document describes the B2B organization management endpoints for GiftFinder AI.

## Overview

Organizations allow companies to manage employee gift recipients, set budgets, and coordinate gift-giving at scale. Each organization has members with different roles (OWNER, ADMIN, MEMBER) that control access permissions.

## Endpoints

### Create Organization

Create a new organization. The creator automatically becomes the OWNER.

**Request:**
```http
POST /api/orgs
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "name": "Acme Corporation",
  "description": "Internal gift management for Acme Corp",
  "giftBudget": 1000.00
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Acme Corporation",
  "description": "Internal gift management for Acme Corp",
  "giftBudget": 1000.00,
  "createdAt": "2026-01-15T20:00:00",
  "updatedAt": "2026-01-15T20:00:00"
}
```

### Add Organization Member

Add a user to an organization with a specific role. Requires OWNER or ADMIN role.

**Request:**
```http
POST /api/orgs/{id}/members
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "userEmail": "john.doe@example.com",
  "role": "MEMBER"
}
```

**Response:**
```json
{
  "id": 1,
  "userId": 42,
  "userEmail": "john.doe@example.com",
  "userName": "John Doe",
  "role": "MEMBER",
  "joinedAt": "2026-01-15T20:05:00"
}
```

### Get Organization Recipients

List all gift recipients associated with an organization. Requires organization membership.

**Request:**
```http
GET /api/orgs/{id}/recipients
Authorization: Bearer <jwt-token>
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Jane Smith",
    "description": "Senior Developer",
    "birthday": "1990-05-15",
    "createdAt": "2026-01-10T10:00:00",
    "updatedAt": "2026-01-10T10:00:00"
  }
]
```

### Get Organization Details

Get organization information. Requires organization membership.

**Request:**
```http
GET /api/orgs/{id}
Authorization: Bearer <jwt-token>
```

**Response:**
```json
{
  "id": 1,
  "name": "Acme Corporation",
  "description": "Internal gift management for Acme Corp",
  "giftBudget": 1000.00,
  "createdAt": "2026-01-15T20:00:00",
  "updatedAt": "2026-01-15T20:00:00"
}
```

## Role-Based Access Control (RBAC)

### Roles

- **OWNER**: Full control over organization, can manage all members and settings
- **ADMIN**: Can manage members and recipients, cannot delete organization
- **MEMBER**: Can view organization recipients, limited write access

### Permissions Matrix

| Action | OWNER | ADMIN | MEMBER |
|--------|-------|-------|--------|
| Create organization | ✓ | - | - |
| Delete organization | ✓ | - | - |
| Add members | ✓ | ✓ | - |
| Remove members | ✓ | ✓ | - |
| View organization | ✓ | ✓ | ✓ |
| View recipients | ✓ | ✓ | ✓ |
| Manage recipients | ✓ | ✓ | - |
| Update budget | ✓ | - | - |

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid request",
  "message": "User is already a member of this organization"
}
```

### 403 Forbidden
```json
{
  "error": "Access denied",
  "message": "Only OWNER or ADMIN can add members"
}
```

### 404 Not Found
```json
{
  "error": "Not found",
  "message": "Organization not found"
}
```

## Integration with Recipients

To associate a recipient with an organization, include the `organizationId` when creating or updating a recipient:

```json
{
  "name": "Jane Smith",
  "description": "Senior Developer",
  "birthday": "1990-05-15",
  "organizationId": 1
}
```

This allows organizations to manage employee birthdays and gift occasions centrally.
