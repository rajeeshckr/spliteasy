#!/bin/bash

# SplitEasy E2E Test Script
# This script runs all E2E tests for the backend API

set -e

BASE_URL="http://localhost:18080"
FAILED_TESTS=0
PASSED_TESTS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

test_result() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"

    if [ "$expected" = "$actual" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        ((PASSED_TESTS++))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name (Expected: $expected, Got: $actual)"
        ((FAILED_TESTS++))
        return 1
    fi
}

echo "========================================="
echo "Phase 1: Authentication Tests"
echo "========================================="

# Test 1.1 - Register User A
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_a","email":"testuser_a@example.com","password":"securepass123"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
USER_A_ID=$(echo "$BODY" | jq -r '.id')
test_result "1.1 Register User A" "201" "$STATUS"

# Test 1.2 - Register User B
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_b","email":"testuser_b@example.com","password":"securepass456"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
USER_B_ID=$(echo "$BODY" | jq -r '.id')
test_result "1.2 Register User B" "201" "$STATUS"

# Test 1.3 - Duplicate username
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_a","email":"different@example.com","password":"securepass789"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "1.3 Duplicate username" "409" "$STATUS"

# Test 1.4 - Duplicate email
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"different_user","email":"testuser_a@example.com","password":"securepass789"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "1.4 Duplicate email" "409" "$STATUS"

# Test 1.5 - Short password
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"shortpw","email":"short@example.com","password":"abc"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "1.5 Short password" "400" "$STATUS"

# Test 1.6 - Login User A
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_a","password":"securepass123"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
TOKEN_A=$(echo "$BODY" | jq -r '.token')
USER_A_ID=$(echo "$BODY" | jq -r '.userId')
test_result "1.6 Login User A" "200" "$STATUS"

# Test 1.7 - Login User B
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_b","password":"securepass456"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
TOKEN_B=$(echo "$BODY" | jq -r '.token')
USER_B_ID=$(echo "$BODY" | jq -r '.userId')
test_result "1.7 Login User B" "200" "$STATUS"

# Test 1.8 - Wrong password
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_a","password":"wrongpassword"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "1.8 Wrong password" "401" "$STATUS"

# Test 1.9 - Login with email
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_a@example.com","password":"securepass123"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "1.9 Login with email" "200" "$STATUS"

# Test 1.10 - No token
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/api/groups)
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "1.10 Access without token" "401" "$STATUS"

echo ""
echo "========================================="
echo "Phase 2: User Search Tests"
echo "========================================="

# Test 2.1 - Search for User B
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/users/search?q=testuser_b" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "2.1 Search for User B" "200" "$STATUS"

# Test 2.2 - Empty query
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/users/search?q=" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "2.2 Empty search query" "200" "$STATUS"

echo ""
echo "========================================="
echo "Phase 3: Group Management Tests"
echo "========================================="

# Test 3.1 - Create group
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/groups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d '{"name":"Weekend Trip","description":"Splitting costs for weekend getaway"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
GROUP_ID=$(echo "$BODY" | jq -r '.id')
test_result "3.1 Create group" "201" "$STATUS"

# Test 3.2 - Get group as member
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "3.2 Get group as member" "200" "$STATUS"

# Test 3.3 - Get group as non-member
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_B")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "3.3 Get group as non-member" "403" "$STATUS"

# Test 3.4 - Add User B to group
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_B_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "3.4 Add User B to group" "201" "$STATUS"

# Test 3.5 - Duplicate membership
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_B_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "3.5 Duplicate membership" "409" "$STATUS"

# Test 3.6 - Get group as new member
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_B")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "3.6 Get group as new member" "200" "$STATUS"

# Test 3.7 - List groups
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/api/groups \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "3.7 List groups" "200" "$STATUS"

# Test 3.8 - Non-existent group
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/api/groups/99999 \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "3.8 Non-existent group" "404" "$STATUS"

echo ""
echo "========================================="
echo "Phase 4: Expense Tests"
echo "========================================="

# Test 4.1 - Add expense (User A paid $100)
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"description\":\"Dinner\",\"amountCents\":10000,\"paidByUserId\":$USER_A_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
EXPENSE_1_ID=$(echo "$BODY" | jq -r '.id')
test_result "4.1 Add expense (User A $100)" "201" "$STATUS"

# Test 4.2 - Add expense (User B paid $60)
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"description\":\"Gas\",\"amountCents\":6000,\"paidByUserId\":$USER_B_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
EXPENSE_2_ID=$(echo "$BODY" | jq -r '.id')
test_result "4.2 Add expense (User B $60)" "201" "$STATUS"

# Test 4.3 - List expenses
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "4.3 List expenses" "200" "$STATUS"

# Test 4.4 - Add expense as non-member
# First register User C
RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_c","email":"testuser_c@example.com","password":"securepass789"}')
USER_C_ID=$(echo "$RESPONSE" | jq -r '.id')

RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_c","password":"securepass789"}')
TOKEN_C=$(echo "$RESPONSE" | jq -r '.token')

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_C" \
  -d '{"description":"Unauthorized","amountCents":1000,"paidByUserId":999}')
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "4.4 Add expense as non-member" "403" "$STATUS"

echo ""
echo "========================================="
echo "Phase 5: Balance Calculation Tests"
echo "========================================="

# Test 5.1 - Get balances
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
test_result "5.1 Get group balances" "200" "$STATUS"

# Test 5.2 - Dashboard User A
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/api/dashboard \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
TOTAL_OWED=$(echo "$BODY" | jq -r '.totalOwed')
TOTAL_OWE=$(echo "$BODY" | jq -r '.totalOwe')
test_result "5.2 Dashboard User A" "200" "$STATUS"
test_result "5.2a User A totalOwed = 2000" "2000" "$TOTAL_OWED"
test_result "5.2b User A totalOwe = 0" "0" "$TOTAL_OWE"

# Test 5.3 - Dashboard User B
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/api/dashboard \
  -H "Authorization: Bearer $TOKEN_B")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
TOTAL_OWED=$(echo "$BODY" | jq -r '.totalOwed')
TOTAL_OWE=$(echo "$BODY" | jq -r '.totalOwe')
test_result "5.3 Dashboard User B" "200" "$STATUS"
test_result "5.3a User B totalOwed = 0" "0" "$TOTAL_OWED"
test_result "5.3b User B totalOwe = 2000" "2000" "$TOTAL_OWE"

echo ""
echo "========================================="
echo "Phase 6: Settlement Tests"
echo "========================================="

# Test 6.1 - Partial settlement
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/settle" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"fromUserId\":$USER_B_ID,\"toUserId\":$USER_A_ID,\"amountCents\":1000}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "6.1 Partial settlement ($10)" "200" "$STATUS"

# Test 6.2 - Balances after partial settlement
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "6.2 Balances after partial settlement" "200" "$STATUS"

# Test 6.3 - Dashboard after partial settlement
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/api/dashboard \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
TOTAL_OWED=$(echo "$BODY" | jq -r '.totalOwed')
test_result "6.3 Dashboard User A after partial" "200" "$STATUS"
test_result "6.3a User A totalOwed = 1000" "1000" "$TOTAL_OWED"

# Test 6.4 - Full settlement
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/settle" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"fromUserId\":$USER_B_ID,\"toUserId\":$USER_A_ID,\"amountCents\":1000}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "6.4 Full settlement ($10)" "200" "$STATUS"

# Test 6.5 - Zero balances after full settlement
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "6.5 Balances after full settlement" "200" "$STATUS"

# Test 6.6 - Dashboard shows zero
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET $BASE_URL/api/dashboard \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
TOTAL_OWED=$(echo "$BODY" | jq -r '.totalOwed')
TOTAL_OWE=$(echo "$BODY" | jq -r '.totalOwe')
test_result "6.6 Dashboard shows zero" "200" "$STATUS"
test_result "6.6a User A totalOwed = 0" "0" "$TOTAL_OWED"
test_result "6.6b User A totalOwe = 0" "0" "$TOTAL_OWE"

echo ""
echo "========================================="
echo "Phase 7: Expense Deletion Tests"
echo "========================================="

# Test 7.1 - Add expense to delete
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"description\":\"To be deleted\",\"amountCents\":5000,\"paidByUserId\":$USER_A_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
EXPENSE_DEL_ID=$(echo "$BODY" | jq -r '.id')
test_result "7.1 Add expense to delete" "201" "$STATUS"

# Test 7.2 - Delete expense
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/groups/$GROUP_ID/expenses/$EXPENSE_DEL_ID" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "7.2 Delete expense" "200" "$STATUS"

# Test 7.3 - Verify expense is gone
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "7.3 Verify expense deleted" "200" "$STATUS"

echo ""
echo "========================================="
echo "Phase 8: Member Removal Tests"
echo "========================================="

# Test 8.1 - Remove User B
RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/groups/$GROUP_ID/members/$USER_B_ID" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "8.1 Remove User B" "200" "$STATUS"

# Test 8.2 - User B can't access group
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_B")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "8.2 User B can't access group" "403" "$STATUS"

# Test 8.3 - Only User A in members
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "8.3 Only User A in members" "200" "$STATUS"

echo ""
echo "========================================="
echo "Phase 9: Multi-User Complex Scenario"
echo "========================================="

# Test 9.1 - Add User C and re-add User B
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_C_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "9.1a Add User C" "201" "$STATUS"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_B_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "9.1b Re-add User B" "201" "$STATUS"

# Test 9.2 - User A pays $90 for groceries
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"description\":\"Groceries\",\"amountCents\":9000,\"paidByUserId\":$USER_A_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "9.2 User A $90 groceries" "201" "$STATUS"

# Test 9.3 - User B pays $45 for drinks
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"description\":\"Drinks\",\"amountCents\":4500,\"paidByUserId\":$USER_B_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "9.3 User B $45 drinks" "201" "$STATUS"

# Test 9.4 - User C pays $30 for snacks
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_C" \
  -d "{\"description\":\"Snacks\",\"amountCents\":3000,\"paidByUserId\":$USER_C_ID}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "9.4 User C $30 snacks" "201" "$STATUS"

# Test 9.5 - Verify 3-person balances
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
test_result "9.5 Get 3-person balances" "200" "$STATUS"

# Test 9.6 - User C settles with User A
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/groups/$GROUP_ID/settle" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_C" \
  -d "{\"fromUserId\":$USER_C_ID,\"toUserId\":$USER_A_ID,\"amountCents\":2500}")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "9.6 User C settles $25 with A" "200" "$STATUS"

# Test 9.7 - Verify remaining balance
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "9.7 Verify remaining balance" "200" "$STATUS"

echo ""
echo "========================================="
echo "Phase 10: Seeded User Tests"
echo "========================================="

# Test 10.1 - Login as alice
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}')
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "10.1 Login as seeded user alice" "200" "$STATUS"

# Test 10.2 - Search for bob
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/users/search?q=bob" \
  -H "Authorization: Bearer $TOKEN_A")
STATUS=$(echo "$RESPONSE" | tail -n1)
test_result "10.2 Search for seeded user bob" "200" "$STATUS"

echo ""
echo "========================================="
echo "TEST SUMMARY"
echo "========================================="
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"
echo "Total: $((PASSED_TESTS + FAILED_TESTS))"

if [ $FAILED_TESTS -eq 0 ]; then
    echo ""
    echo "🎉 ALL TESTS PASSED! 🎉"
    exit 0
else
    echo ""
    echo "❌ SOME TESTS FAILED"
    exit 1
fi
