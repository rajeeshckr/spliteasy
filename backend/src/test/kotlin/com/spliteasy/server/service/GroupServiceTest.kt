package com.spliteasy.server.service

import com.spliteasy.server.dto.SettleRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupServiceTest {

    @BeforeEach
    fun setup() {
        TestDbHelper.initTestDb()
    }

    @AfterEach
    fun teardown() {
        TestDbHelper.cleanAll()
    }

    @Test
    fun `getUserGroups shows correct balance before settlement`() {
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        val expense = TestDbHelper.createExpense(group, "Dinner", 10000, alice)
        TestDbHelper.createSplit(expense, alice, 5000)
        TestDbHelper.createSplit(expense, bob, 5000)

        // Alice is owed $50 (paid $100, her share is $50)
        val aliceGroups = GroupService.getUserGroups(alice)
        assertEquals(1, aliceGroups.size)
        assertEquals(5000, aliceGroups[0].myBalance)

        // Bob owes $50 (paid $0, his share is $50)
        val bobGroups = GroupService.getUserGroups(bob)
        assertEquals(1, bobGroups.size)
        assertEquals(-5000, bobGroups[0].myBalance)
    }

    @Test
    fun `getUserGroups shows zero balance after settlement`() {
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)

        val expense = TestDbHelper.createExpense(group, "Dinner", 10000, alice)
        TestDbHelper.createSplit(expense, alice, 5000)
        TestDbHelper.createSplit(expense, bob, 5000)

        // Settle Bob's debt
        BalanceService.settleDebt(group, SettleRequest(bob, alice, 5000))

        // Alice balance should be 0
        val aliceGroups = GroupService.getUserGroups(alice)
        assertEquals(0, aliceGroups[0].myBalance)

        // Bob balance should be 0
        val bobGroups = GroupService.getUserGroups(bob)
        assertEquals(0, bobGroups[0].myBalance)
    }

    @Test
    fun `getUserGroups shows partial balance after partial group settlement`() {
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val carol = TestDbHelper.createUser(username = "carol", email = "carol@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)
        TestDbHelper.addMemberToGroup(group, bob)
        TestDbHelper.addMemberToGroup(group, carol)

        // Alice pays $60 split 3 ways ($20 each)
        val expense = TestDbHelper.createExpense(group, "Dinner", 6000, alice)
        TestDbHelper.createSplit(expense, alice, 2000)
        TestDbHelper.createSplit(expense, bob, 2000)
        TestDbHelper.createSplit(expense, carol, 2000)

        // Before: Alice is owed $40 (paid $60, her share $20)
        val aliceBefore = GroupService.getUserGroups(alice)
        assertEquals(4000, aliceBefore[0].myBalance)

        // Bob settles
        BalanceService.settleDebt(group, SettleRequest(bob, alice, 2000))

        // After: Alice is now owed only $20 (from Carol)
        val aliceAfter = GroupService.getUserGroups(alice)
        assertEquals(2000, aliceAfter[0].myBalance)

        // Bob should be at 0
        val bobAfter = GroupService.getUserGroups(bob)
        assertEquals(0, bobAfter[0].myBalance)

        // Carol still owes $20
        val carolAfter = GroupService.getUserGroups(carol)
        assertEquals(-2000, carolAfter[0].myBalance)
    }

    @Test
    fun `isUserInGroup returns correct result`() {
        val alice = TestDbHelper.createUser(username = "alice", email = "alice@test.com")
        val bob = TestDbHelper.createUser(username = "bob", email = "bob@test.com")
        val group = TestDbHelper.createGroup("Trip", alice)

        assertTrue(GroupService.isUserInGroup(group, alice))
        assertTrue(!GroupService.isUserInGroup(group, bob))

        TestDbHelper.addMemberToGroup(group, bob)
        assertTrue(GroupService.isUserInGroup(group, bob))
    }
}
