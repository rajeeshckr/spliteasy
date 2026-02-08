# SplitEasy Backend End-to-End API Test Prompt

> **Ralph Wiggum compatible prompt** — Use with `cat BACKEND_E2E_TEST_PROMPT.md | claude --continue`
> or in a Ralph loop: `/ralph-loop "$(cat BACKEND_E2E_TEST_PROMPT.md)" --completion-promise "ALL E2E TESTS PASSED" --max-iterations 5`

---

## Objective

You are an end-to-end test engineer for the **SplitEasy** backend API. Your job is to build the backend Docker container, start it, and execute a comprehensive suite of API tests against it using `curl`. You must verify every response status code AND response body. If any test fails, investigate the issue, fix the backend code, rebuild the container, and re-run the tests.

When ALL tests pass, output: `<promise>ALL E2E TESTS PASSED</promise>`

---

## Phase 0: Environment Setup

1. Navigate to the `backend/` directory of this project.
2. Build the Docker image (use docker or podman, whichever is available):
   ```bash
   docker build -t spliteasy-e2e-test:latest -f Containerfile . 2>&1
   # or: podman build -t spliteasy-e2e-test:latest -f Containerfile . 2>&1
   ```
3. Stop and remove any existing test container:
   ```bash
   docker rm -f spliteasy-e2e-test 2>/dev/null || true
   ```
4. Start a fresh container with a clean database (no volume mount = fresh SQLite each run):
   ```bash
   docker run -d --name spliteasy-e2e-test -p 18080:8080 spliteasy-e2e-test:latest
   ```
   - Use port **18080** on the host to avoid conflicts.
5. Wait for the container to be healthy (poll up to 60 seconds):
   ```bash
   for i in $(seq 1 30); do
     curl -sf http://localhost:18080/ && break || sleep 2
   done
   ```
6. Verify the container is running: `docker ps --filter name=spliteasy-e2e-test`

**If the build fails**, read the error output, fix the code, and rebuild. Do NOT proceed with tests until the container is running.

---

## Phase 1: Authentication Tests

### Test 1.1 — Register User A
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_a","email":"testuser_a@example.com","password":"securepass123"}'
```
**Expected:** HTTP `201`. Response body contains `"id"`, `"username":"testuser_a"`, `"email":"testuser_a@example.com"`. No password in response.

### Test 1.2 — Register User B
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_b","email":"testuser_b@example.com","password":"securepass456"}'
```
**Expected:** HTTP `201`. Response body contains `"id"`, `"username":"testuser_b"`.

### Test 1.3 — Duplicate Registration (same username)
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_a","email":"different@example.com","password":"securepass789"}'
```
**Expected:** HTTP `409` Conflict.

### Test 1.4 — Duplicate Registration (same email)
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"different_user","email":"testuser_a@example.com","password":"securepass789"}'
```
**Expected:** HTTP `409` Conflict.

### Test 1.5 — Registration with short password
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"shortpw","email":"short@example.com","password":"abc"}'
```
**Expected:** HTTP `400` Bad Request (password too short, min 8 chars).

### Test 1.6 — Login User A
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_a","password":"securepass123"}'
```
**Expected:** HTTP `200`. Response contains `"token"` (JWT string), `"userId"`, `"username":"testuser_a"`.
**Action:** Save the token as `TOKEN_A` and the userId as `USER_A_ID`.

### Test 1.7 — Login User B
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_b","password":"securepass456"}'
```
**Expected:** HTTP `200`. Response contains `"token"`, `"userId"`, `"username":"testuser_b"`.
**Action:** Save the token as `TOKEN_B` and the userId as `USER_B_ID`.

### Test 1.8 — Login with wrong password
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_a","password":"wrongpassword"}'
```
**Expected:** HTTP `401` Unauthorized.

### Test 1.9 — Login with email instead of username
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_a@example.com","password":"securepass123"}'
```
**Expected:** HTTP `200`. Response contains valid token.

### Test 1.10 — Access protected endpoint without token
```bash
curl -s -w "\n%{http_code}" -X GET http://localhost:18080/api/groups
```
**Expected:** HTTP `401` Unauthorized.

---

## Phase 2: User Search Tests

### Test 2.1 — Search for User B (as User A)
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/users/search?q=testuser_b" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Response is a JSON array containing at least one user with `"username":"testuser_b"`.

### Test 2.2 — Search with empty query
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/users/search?q=" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Response is a JSON array (may return all users or empty).

---

## Phase 3: Group Management Tests

### Test 3.1 — Create a group (as User A)
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/groups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d '{"name":"Weekend Trip","description":"Splitting costs for weekend getaway"}'
```
**Expected:** HTTP `201`. Response contains `"id"`, `"name":"Weekend Trip"`, `"description"`, `"creatorId"` matching User A's ID.
**Action:** Save the group id as `GROUP_ID`.

### Test 3.2 — Get group details (as User A, who is a member)
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Response contains group info, `"members"` array with User A, and `"creatorId"`.

### Test 3.3 — Get group details (as User B, NOT a member yet)
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_B"
```
**Expected:** HTTP `403` Forbidden.

### Test 3.4 — Add User B to the group (as User A)
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_B_ID}"
```
**Expected:** HTTP `201`. Response contains `"message":"Member added"`.

### Test 3.5 — Add User B again (duplicate membership)
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_B_ID}"
```
**Expected:** HTTP `409` Conflict.

### Test 3.6 — Get group details (as User B, now a member)
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_B"
```
**Expected:** HTTP `200`. Members array contains both User A and User B.

### Test 3.7 — List groups (as User A)
```bash
curl -s -w "\n%{http_code}" -X GET http://localhost:18080/api/groups \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. JSON array containing at least the "Weekend Trip" group with `"memberCount":2` and `"myBalance":0` (no expenses yet).

### Test 3.8 — Get non-existent group
```bash
curl -s -w "\n%{http_code}" -X GET http://localhost:18080/api/groups/99999 \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `404` Not Found.

---

## Phase 4: Expense Tests

### Test 4.1 — Add expense: User A paid $100 dinner (split equally between A & B)
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"description\":\"Dinner\",\"amountCents\":10000,\"paidByUserId\":$USER_A_ID}"
```
**Expected:** HTTP `201`. Response contains:
- `"description":"Dinner"`, `"amountCents":10000`
- `"paidBy"` object with User A's info
- `"splits"` array with 2 entries, each `"shareAmountCents":5000`
**Action:** Save the expense id as `EXPENSE_1_ID`.

### Test 4.2 — Add expense: User B paid $60 for gas
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"description\":\"Gas\",\"amountCents\":6000,\"paidByUserId\":$USER_B_ID}"
```
**Expected:** HTTP `201`. Splits show 2 entries of `3000` cents each.
**Action:** Save the expense id as `EXPENSE_2_ID`.

### Test 4.3 — List expenses for the group
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. JSON array with 2 expenses (Dinner and Gas).

### Test 4.4 — Add expense as non-member (User B creates another group and User A tries to add expense)
First register and login a third user (User C) who is NOT in the group:
```bash
curl -s -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser_c","email":"testuser_c@example.com","password":"securepass789"}'

curl -s -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser_c","password":"securepass789"}'
```
Save User C's token as `TOKEN_C`. Then:
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_C" \
  -d '{"description":"Unauthorized","amountCents":1000,"paidByUserId":999}'
```
**Expected:** HTTP `403` Forbidden.

---

## Phase 5: Balance Calculation Tests

### Test 5.1 — Get balances for the group
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Response contains `"groupName":"Weekend Trip"` and `"balances"` array.

**Math verification (CRITICAL — verify these calculations):**
- User A paid $100, split = $50 each → User B owes User A $50
- User B paid $60, split = $30 each → User A owes User B $30
- **Net: User B owes User A $20** ($50 - $30 = $20, i.e., 2000 cents)
- The balances array should show a single simplified debt: `fromUser` = User B, `toUser` = User A, `amountCents` = 2000.

### Test 5.2 — Get dashboard (as User A)
```bash
curl -s -w "\n%{http_code}" -X GET http://localhost:18080/api/dashboard \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Response contains:
- `"totalOwed": 2000` (User B owes User A $20)
- `"totalOwe": 0` (User A doesn't owe anyone)
- `"groups"` array with the Weekend Trip group showing `"myBalance": 2000` (positive = money owed TO User A)

### Test 5.3 — Get dashboard (as User B)
```bash
curl -s -w "\n%{http_code}" -X GET http://localhost:18080/api/dashboard \
  -H "Authorization: Bearer $TOKEN_B"
```
**Expected:** HTTP `200`. Response contains:
- `"totalOwed": 0`
- `"totalOwe": 2000` (User B owes $20)
- `"groups"` array with `"myBalance": -2000` (negative = User B owes money)

---

## Phase 6: Settlement Tests

### Test 6.1 — Partial settlement: User B pays User A $10
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/settle" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"fromUserId\":$USER_B_ID,\"toUserId\":$USER_A_ID,\"amountCents\":1000}"
```
**Expected:** HTTP `200`. Response contains `"message":"Settlement recorded"`.

### Test 6.2 — Verify balances after partial settlement
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Remaining balance: User B owes User A $10 (1000 cents).

### Test 6.3 — Dashboard after partial settlement (User A)
```bash
curl -s -w "\n%{http_code}" -X GET http://localhost:18080/api/dashboard \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** `"totalOwed": 1000`, `"totalOwe": 0`.

### Test 6.4 — Full settlement: User B pays User A remaining $10
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/settle" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"fromUserId\":$USER_B_ID,\"toUserId\":$USER_A_ID,\"amountCents\":1000}"
```
**Expected:** HTTP `200`. Settlement recorded.

### Test 6.5 — Verify zero balances after full settlement
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Balances array should be empty or all amounts should be 0.

### Test 6.6 — Dashboard shows zero after full settlement
```bash
curl -s -w "\n%{http_code}" -X GET http://localhost:18080/api/dashboard \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** `"totalOwed": 0`, `"totalOwe": 0`.

---

## Phase 7: Expense Deletion Tests

### Test 7.1 — Add another expense then delete it
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"description\":\"To be deleted\",\"amountCents\":5000,\"paidByUserId\":$USER_A_ID}"
```
**Expected:** HTTP `201`. Save expense id as `EXPENSE_DEL_ID`.

### Test 7.2 — Delete the expense
```bash
curl -s -w "\n%{http_code}" -X DELETE "http://localhost:18080/api/groups/$GROUP_ID/expenses/$EXPENSE_DEL_ID" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Response contains `"message":"Expense deleted"`.

### Test 7.3 — Verify expense is gone
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. The "To be deleted" expense should NOT be in the list.

---

## Phase 8: Member Removal Tests

### Test 8.1 — Remove User B from the group
```bash
curl -s -w "\n%{http_code}" -X DELETE "http://localhost:18080/api/groups/$GROUP_ID/members/$USER_B_ID" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Response contains `"message":"Member removed"`.

### Test 8.2 — Verify User B can no longer access the group
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_B"
```
**Expected:** HTTP `403` Forbidden.

### Test 8.3 — Verify group members list only shows User A
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Members array contains only User A.

---

## Phase 9: Multi-User Complex Scenario

This tests a realistic 3-person group scenario end to end.

### Test 9.1 — Add User C to the group
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_C_ID}"
```
Also re-add User B:
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"userId\":$USER_B_ID}"
```
**Expected:** Both return HTTP `201`.

### Test 9.2 — User A pays $90 for groceries (split 3 ways = $30 each)
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d "{\"description\":\"Groceries\",\"amountCents\":9000,\"paidByUserId\":$USER_A_ID}"
```
**Expected:** HTTP `201`. Splits: 3 entries of 3000 cents each.

### Test 9.3 — User B pays $45 for drinks (split 3 ways = $15 each)
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_B" \
  -d "{\"description\":\"Drinks\",\"amountCents\":4500,\"paidByUserId\":$USER_B_ID}"
```
**Expected:** HTTP `201`. Splits: 3 entries of 1500 cents each.

### Test 9.4 — User C pays $30 for snacks (split 3 ways = $10 each)
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/expenses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_C" \
  -d "{\"description\":\"Snacks\",\"amountCents\":3000,\"paidByUserId\":$USER_C_ID}"
```
**Expected:** HTTP `201`. Splits: 3 entries of 1000 cents each.

### Test 9.5 — Verify 3-person balances
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected math (CRITICAL — verify these calculations carefully):**

Each person's total share across all 3 expenses:
- Groceries ($90 split 3 ways): $30 each
- Drinks ($45 split 3 ways): $15 each
- Snacks ($30 split 3 ways): $10 each
- **Total share per person: $55 (5500 cents)**

Net balances (paid minus share):
- **User A:** paid $90 - share $55 = **+$35 (owed 3500 cents)**
- **User B:** paid $45 - share $55 = **-$10 (owes 1000 cents)**
- **User C:** paid $30 - share $55 = **-$25 (owes 2500 cents)**
- **Checksum:** +3500 - 1000 - 2500 = 0 ✓

**Simplified debts (minimal transactions):**
- B owes A **1000 cents** ($10)
- C owes A **2500 cents** ($25)

**Verify:** The sum of all `fromUser` amounts should equal the sum of all `toUser` amounts, and net balances should be zero across all users.

### Test 9.6 — Settle User C's debt to User A
```bash
curl -s -w "\n%{http_code}" -X POST "http://localhost:18080/api/groups/$GROUP_ID/settle" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_C" \
  -d "{\"fromUserId\":$USER_C_ID,\"toUserId\":$USER_A_ID,\"amountCents\":2500}"
```
**Expected:** HTTP `200`. Settlement recorded.

### Test 9.7 — Verify balances after C settles with A
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/groups/$GROUP_ID/balances" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** Only remaining debt is B owes A 1000 cents.

---

## Phase 10: Seeded User Tests

The backend seeds test users (alice, bob, carol, dave, eve) on first run. Verify they exist.

### Test 10.1 — Login as seeded user alice
```bash
curl -s -w "\n%{http_code}" -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"alice","password":"password1"}'
```
**Expected:** HTTP `200`. Valid login response with token.

### Test 10.2 — Search for seeded users
```bash
curl -s -w "\n%{http_code}" -X GET "http://localhost:18080/api/users/search?q=bob" \
  -H "Authorization: Bearer $TOKEN_A"
```
**Expected:** HTTP `200`. Results include user `bob`.

---

## Cleanup

After all tests complete:
```bash
docker stop spliteasy-e2e-test && docker rm spliteasy-e2e-test
```

---

## Test Reporting

After running all tests, produce a summary table:

| Phase | Test | Status | Notes |
|-------|------|--------|-------|
| 1. Auth | 1.1 Register User A | PASS/FAIL | ... |
| ... | ... | ... | ... |

Count: `X/Y tests passed`.

### If ALL tests pass:
```
<promise>ALL E2E TESTS PASSED</promise>
```

### If ANY test fails:
1. Document which test(s) failed and why.
2. Read the relevant backend source code to understand the issue.
3. Fix the code.
4. Rebuild the Docker image and restart the container.
5. Re-run ALL tests from the beginning.
6. Repeat until all tests pass.

---

## Important Notes

- **Always use port 18080** to avoid conflicts with any locally running instance.
- **Never hardcode user IDs** — always extract them from registration/login responses.
- **Verify response bodies**, not just status codes.
- **Check math carefully** — balance calculations are the most likely source of bugs.
- **Keep the container logs** — if something fails, check `docker logs spliteasy-e2e-test`.
- The backend uses SQLite, so the database is reset on each fresh container start (no volume).
- JWT tokens expire after 7 days, so they'll be valid for the duration of testing.
- All monetary amounts are in **cents** (e.g., 10000 = $100.00).
