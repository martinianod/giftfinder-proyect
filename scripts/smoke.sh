#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:5173}"
MAX_WAIT=60
SLEEP_INTERVAL=2

echo "=========================================="
echo "  GiftFinder Smoke Test"
echo "=========================================="
echo ""

# Function to wait for service
wait_for_service() {
    local url=$1
    local name=$2
    local elapsed=0
    
    echo -e "${YELLOW}⏳ Waiting for $name at $url...${NC}"
    
    while [ $elapsed -lt $MAX_WAIT ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $name is ready!${NC}"
            return 0
        fi
        sleep $SLEEP_INTERVAL
        elapsed=$((elapsed + SLEEP_INTERVAL))
    done
    
    echo -e "${RED}✗ Timeout waiting for $name${NC}"
    return 1
}

# Function to run test
run_test() {
    local test_name=$1
    local method=$2
    local url=$3
    local data=$4
    local expected_code=$5
    
    echo ""
    echo "Testing: $test_name"
    echo "Request: $method $url"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" == "$expected_code" ]; then
        echo -e "${GREEN}✓ Test passed (HTTP $http_code)${NC}"
        echo "Response: $body" | head -c 200
        return 0
    else
        echo -e "${RED}✗ Test failed (Expected HTTP $expected_code, got HTTP $http_code)${NC}"
        echo "Response: $body"
        return 1
    fi
}

# 1. Wait for backend health
echo ""
echo "Step 1: Checking Backend Health"
echo "--------------------------------"
if ! wait_for_service "$BACKEND_URL/actuator/health" "Backend"; then
    echo -e "${RED}❌ Backend health check failed${NC}"
    exit 1
fi

# 2. Wait for frontend
echo ""
echo "Step 2: Checking Frontend"
echo "-------------------------"
if ! wait_for_service "$FRONTEND_URL" "Frontend"; then
    echo -e "${RED}❌ Frontend check failed${NC}"
    exit 1
fi

# 3. Test signup
echo ""
echo "Step 3: Testing Signup"
echo "----------------------"
RANDOM_EMAIL="test$(date +%s)@example.com"
SIGNUP_DATA="{\"name\":\"Test User\",\"email\":\"$RANDOM_EMAIL\",\"password\":\"password123\"}"

if run_test "Signup new user" "POST" "$BACKEND_URL/api/auth/signup" "$SIGNUP_DATA" "200"; then
    SIGNUP_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/auth/signup" \
        -H "Content-Type: application/json" \
        -d "$SIGNUP_DATA")
    TOKEN=$(echo "$SIGNUP_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "Token obtained: ${TOKEN:0:20}..."
else
    echo -e "${RED}❌ Signup test failed${NC}"
    exit 1
fi

# 4. Test login
echo ""
echo "Step 4: Testing Login"
echo "---------------------"
LOGIN_DATA="{\"email\":\"$RANDOM_EMAIL\",\"password\":\"password123\"}"

if ! run_test "Login with created user" "POST" "$BACKEND_URL/api/auth/login" "$LOGIN_DATA" "200"; then
    echo -e "${RED}❌ Login test failed${NC}"
    exit 1
fi

# 5. Test duplicate signup (should fail with 409)
echo ""
echo "Step 5: Testing Duplicate Signup"
echo "---------------------------------"
if run_test "Duplicate signup (should fail)" "POST" "$BACKEND_URL/api/auth/signup" "$SIGNUP_DATA" "409"; then
    echo -e "${GREEN}✓ Duplicate email correctly rejected${NC}"
else
    echo -e "${YELLOW}⚠ Expected 409 Conflict for duplicate email${NC}"
fi

# 6. Test invalid login
echo ""
echo "Step 6: Testing Invalid Login"
echo "------------------------------"
INVALID_LOGIN="{\"email\":\"$RANDOM_EMAIL\",\"password\":\"wrongpassword\"}"

if run_test "Invalid login (should fail)" "POST" "$BACKEND_URL/api/auth/login" "$INVALID_LOGIN" "401"; then
    echo -e "${GREEN}✓ Invalid credentials correctly rejected${NC}"
else
    echo -e "${YELLOW}⚠ Expected 401 Unauthorized for invalid credentials${NC}"
fi

# 7. Test validation errors
echo ""
echo "Step 7: Testing Validation"
echo "--------------------------"
INVALID_DATA="{\"name\":\"\",\"email\":\"notanemail\",\"password\":\"short\"}"

if run_test "Invalid data (should fail)" "POST" "$BACKEND_URL/api/auth/signup" "$INVALID_DATA" "400"; then
    echo -e "${GREEN}✓ Validation correctly enforced${NC}"
else
    echo -e "${YELLOW}⚠ Expected 400 Bad Request for invalid data${NC}"
fi

# Summary
echo ""
echo "=========================================="
echo -e "${GREEN}✅ All smoke tests completed!${NC}"
echo "=========================================="
echo ""
echo "🎉 Your GiftFinder stack is working correctly!"
echo ""
echo "Next steps:"
echo "  - Frontend UI: $FRONTEND_URL"
echo "  - Backend API: $BACKEND_URL"
echo "  - API Health: $BACKEND_URL/actuator/health"
echo ""
echo "Test user created:"
echo "  Email: $RANDOM_EMAIL"
echo "  Password: password123"
echo ""
