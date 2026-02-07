package com.spliteasy.app.presentation.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Int,
    onBackClick: () -> Unit,
    onAddExpense: () -> Unit,
    onAddMembers: () -> Unit,
    viewModel: GroupDetailViewModel = viewModel()
) {
    val currentGroupId = remember { groupId }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
        viewModel.loadExpenses(groupId)
        viewModel.loadBalances(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddMembers) {
                        Icon(Icons.Default.PersonAdd, "Add Members")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Default.Add, "Add Expense")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadGroup(groupId) }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.group != null -> {
                    GroupDetailContent(
                        uiState = uiState,
                        onSettleDebt = { fromUserId, toUserId, amount ->
                            viewModel.settleDebt(currentGroupId, fromUserId, toUserId, amount)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupDetailContent(
    uiState: GroupDetailUiState,
    onSettleDebt: (Int, Int, Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Group Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.group?.description != null) {
                    Text(
                        text = uiState.group.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "${uiState.group?.members?.size ?: 0} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Expenses") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Balances") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Members") }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> ExpensesTab(uiState.expenses)
            1 -> BalancesTab(uiState.balances, onSettleDebt)
            2 -> MembersTab(uiState.group?.members ?: emptyList())
        }
    }
}

@Composable
private fun ExpensesTab(expenses: List<com.spliteasy.app.data.dto.ExpenseResponse>) {
    if (expenses.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No expenses yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(expenses) { expense ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$${expense.amountCents / 100.0}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paid by ${expense.paidBy.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancesTab(
    balances: com.spliteasy.app.data.dto.BalanceResponse?,
    onSettleDebt: (Int, Int, Long) -> Unit
) {
    if (balances == null || balances.balances.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "All settled up!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(balances.balances) { balance ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${balance.fromUser.username} owes ${balance.toUser.username}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "$${balance.amountCents / 100.0}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(onClick = {
                            onSettleDebt(
                                balance.fromUser.id,
                                balance.toUser.id,
                                balance.amountCents
                            )
                        }) {
                            Text("Settle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MembersTab(members: List<com.spliteasy.app.data.dto.UserDto>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(members) { member ->
            ListItem(
                headlineContent = { Text(member.username) },
                supportingContent = { Text(member.email) },
                leadingContent = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )
            Divider()
        }
    }
}
