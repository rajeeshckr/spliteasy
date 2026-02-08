package com.spliteasy.server.service

import com.spliteasy.server.dto.SettleRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BalanceServiceTest {

    @BeforeEach
    fun setup() {
        TestDbHelper.initTestDb()
    }

    @AfterEach
    fun teardown() {
        TestDbHelper.cleanAll()
    }

    @Test
    fun `getGroupBalances returns correct balances for simple two-person split`() {
        // Alice pays $100, split equally between Alice and Bob
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        val expense = TestDbHelper.createExpense(group, "Dinner", 10000, alice)
        TestDbHelper.createSplit(expense, alice, 5000)
        TestDbHelper.createSplit(expense, bob, 5000)

        val result = BalanceService.getGroupBalances(group)

        assertEquals(1, result.balances.size)
        val balance = result.balances[0]
        assertEquals(bob, balance.fromUser.id)
        assertEquals(alice, balance.toUser.id)
        assertEquals(5000, balance.amountCents)
    }

    @Test
    fun `getGroupBalances returns empty after full settlement`() {
        // Alice pays $100 split equally, then Bob settles
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        val expense = TestDbHelper.createExpense(group, "Dinner", 10000, alice)
        TestDbHelper.createSplit(expense, alice, 5000)
        TestDbHelper.createSplit(expense, bob, 5000)

        // Settle Bob's debt
        val settleResult = BalanceService.settleDebt(group, SettleRequest(bob, alice, 5000))
        assertTrue(settleResult.isSuccess)

        // Balances should now be empty
        val result = BalanceService.getGroupBalances(group)
        assertTrue(result.balances.isEmpty(), "Expected no balances after settlement, got: ${result.balances}")
    }

    @Test
    fun `settleDebt marks splits as settled and balances update accordingly`() {
        // Alice pays $60, split among Alice, Bob, Carol ($20 each)
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val carol = TestDbHelper.createUser(username = "carol", email = "carol@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)
        TestDbHelper.addMemberToGroup(group, carol)

        val expense = TestDbHelper.createExpense(group, "Dinner", 6000, alice)
        TestDbHelper.createSplit(expense, alice, 2000)
        TestDbHelper.createSplit(expense, bob, 2000)
        TestDbHelper.createSplit(expense, carol, 2000)

        // Before settlement: Bob owes $20, Carol owes $20
        val before = BalanceService.getGroupBalances(group)
        assertEquals(2, before.balances.size)

        // Bob settles his debt
        BalanceService.settleDebt(group, SettleRequest(bob, alice, 2000))

        // After settlement: only Carol owes $20
        val after = BalanceService.getGroupBalances(group)
        assertEquals(1, after.balances.size)
        val remaining = after.balances[0]
        assertEquals(carol, remaining.fromUser.id)
        assertEquals(alice, remaining.toUser.id)
        assertEquals(2000, remaining.amountCents)
    }

    @Test
    fun `settleDebt handles multiple expenses in the same group`() {
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        // Expense 1: Alice pays $100, split equally
        val exp1 = TestDbHelper.createExpense(group, "Dinner", 10000, alice)
        TestDbHelper.createSplit(exp1, alice, 5000)
        TestDbHelper.createSplit(exp1, bob, 5000)

        // Expense 2: Alice pays $40, split equally
        val exp2 = TestDbHelper.createExpense(group, "Taxi", 4000, alice)
        TestDbHelper.createSplit(exp2, alice, 2000)
        TestDbHelper.createSplit(exp2, bob, 2000)

        // Before settlement: Bob owes $70 total
        val before = BalanceService.getGroupBalances(group)
        assertEquals(1, before.balances.size)
        assertEquals(7000, before.balances[0].amountCents)

        // Settle all of Bob's debt
        BalanceService.settleDebt(group, SettleRequest(bob, alice, 7000))

        // After settlement: no balances
        val after = BalanceService.getGroupBalances(group)
        assertTrue(after.balances.isEmpty(), "Expected no balances after full settlement")
    }

    @Test
    fun `getGroupBalances handles cross-payments correctly`() {
        // Alice pays $100 split equally, Bob pays $60 split equally
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        val exp1 = TestDbHelper.createExpense(group, "Dinner", 10000, alice)
        TestDbHelper.createSplit(exp1, alice, 5000)
        TestDbHelper.createSplit(exp1, bob, 5000)

        val exp2 = TestDbHelper.createExpense(group, "Drinks", 6000, bob)
        TestDbHelper.createSplit(exp2, alice, 3000)
        TestDbHelper.createSplit(exp2, bob, 3000)

        // Net: Alice is owed $50-$30 = $20 by Bob
        val result = BalanceService.getGroupBalances(group)
        assertEquals(1, result.balances.size)
        assertEquals(bob, result.balances[0].fromUser.id)
        assertEquals(alice, result.balances[0].toUser.id)
        assertEquals(2000, result.balances[0].amountCents)
    }

    @Test
    fun `getGroupBalances with no expenses returns empty`() {
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        val result = BalanceService.getGroupBalances(group)
        assertTrue(result.balances.isEmpty())
    }

    @Test
    fun `getDashboard reflects settled debts`() {
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        val expense = TestDbHelper.createExpense(group, "Dinner", 10000, alice)
        TestDbHelper.createSplit(expense, alice, 5000)
        TestDbHelper.createSplit(expense, bob, 5000)

        // Before settlement: Bob owes $50
        val dashBefore = BalanceService.getDashboard(bob)
        assertEquals(5000, dashBefore.totalOwe)
        assertEquals(0, dashBefore.totalOwed)

        // Settle
        BalanceService.settleDebt(group, SettleRequest(bob, alice, 5000))

        // After settlement: Bob owes nothing
        val dashAfter = BalanceService.getDashboard(bob)
        assertEquals(0, dashAfter.totalOwe)
        assertEquals(0, dashAfter.totalOwed)
    }
}
