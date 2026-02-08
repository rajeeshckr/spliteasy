import { useEffect, useState, Fragment } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Alert,
  Button,
  Fab,
  Tabs,
  Tab,
  Stack,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Divider,
  IconButton,
  Chip,
} from '@mui/material';
import {
  Add,
  Person,
  PersonAdd,
  CheckCircle,
  Receipt,
} from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { getGroup } from '../api/groups';
import { getExpenses } from '../api/expenses';
import { getBalances, settle } from '../api/balances';
import type { Group } from '../api/groups';
import type { Expense } from '../api/expenses';
import type { BalancesResponse } from '../api/balances';
import { useToast } from '../hooks/useToast';
import { formatCents } from '../utils/format';

export function GroupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const [group, setGroup] = useState<Group | null>(null);
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [balances, setBalances] = useState<BalancesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tab, setTab] = useState(0);

  const loadGroupData = async () => {
    if (!id) return;

    setLoading(true);
    setError('');

    try {
      const [groupData, expensesData, balancesData] = await Promise.all([
        getGroup(parseInt(id, 10)),
        getExpenses(parseInt(id, 10)),
        getBalances(parseInt(id, 10)),
      ]);
      setGroup(groupData);
      setExpenses(expensesData);
      setBalances(balancesData);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load group data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGroupData();
  }, [id]);

  const handleSettle = async (fromUserId: number, toUserId: number, amountCents: number) => {
    if (!id) return;

    try {
      await settle(parseInt(id, 10), { fromUserId, toUserId, amountCents });
      showSuccess('Debt settled successfully!');
      loadGroupData();
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to settle debt');
    }
  };

  if (loading) {
    return (
      <Layout title="Loading..." showBack>
        <LoadingSpinner />
      </Layout>
    );
  }

  if (error || !group) {
    return (
      <Layout title="Error" showBack>
        <Alert severity="error" sx={{ mb: 2 }}>
          {error || 'Group not found'}
        </Alert>
        <Button variant="outlined" onClick={loadGroupData}>
          Retry
        </Button>
      </Layout>
    );
  }

  return (
    <Layout
      title={group.name}
      showBack
      rightAction={
        <IconButton
          color="inherit"
          onClick={() => navigate(`/group/${id}/add-member`)}
        >
          <PersonAdd />
        </IconButton>
      }
    >
      <Box sx={{ pb: 10 }}>
        <Card sx={{ mb: 3 }}>
          <CardContent>
            {group.description && (
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {group.description}
              </Typography>
            )}
            <Chip
              icon={<Person />}
              label={`${group.members.length} member${group.members.length !== 1 ? 's' : ''}`}
              size="small"
              sx={{ mt: 1 }}
            />
          </CardContent>
        </Card>

        <Tabs
          value={tab}
          onChange={(_, newValue) => setTab(newValue)}
          variant="fullWidth"
          sx={{ mb: 3 }}
        >
          <Tab label="Expenses" />
          <Tab label="Balances" />
          <Tab label="Members" />
        </Tabs>

        {tab === 0 && (
          <Box>
            {expenses.length === 0 ? (
              <EmptyState
                icon={<Receipt fontSize="inherit" />}
                message="No expenses yet"
              />
            ) : (
              <Stack spacing={2}>
                {expenses.map((expense) => (
                  <Card key={expense.id}>
                    <CardContent>
                      <Typography variant="subtitle1" fontWeight="medium" gutterBottom>
                        {expense.description}
                      </Typography>
                      <Typography variant="h6" color="primary" gutterBottom>
                        {formatCents(expense.amountCents)}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Paid by {expense.paidBy.username}
                      </Typography>
                    </CardContent>
                  </Card>
                ))}
              </Stack>
            )}
          </Box>
        )}

        {tab === 1 && balances && (
          <Box>
            {balances.balances.length === 0 ? (
              <EmptyState
                icon={<CheckCircle fontSize="inherit" />}
                message="All settled up!"
              />
            ) : (
              <Stack spacing={2}>
                {balances.balances.map((balance, index) => (
                  <Card key={index}>
                    <CardContent>
                      <Box
                        sx={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          mb: 1,
                        }}
                      >
                        <Typography variant="body1">
                          {balance.fromUser.username} owes {balance.toUser.username}
                        </Typography>
                        <Typography
                          variant="body1"
                          fontWeight="bold"
                          color="error"
                        >
                          {formatCents(balance.amountCents)}
                        </Typography>
                      </Box>
                      <Button
                        variant="outlined"
                        size="small"
                        onClick={() =>
                          handleSettle(
                            balance.fromUser.id,
                            balance.toUser.id,
                            balance.amountCents
                          )
                        }
                      >
                        Settle
                      </Button>
                    </CardContent>
                  </Card>
                ))}
              </Stack>
            )}
          </Box>
        )}

        {tab === 2 && (
          <List>
            {group.members.map((member, index) => (
              <Fragment key={member.id}>
                {index > 0 && <Divider />}
                <ListItem>
                  <ListItemAvatar>
                    <Avatar>
                      <Person />
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={member.username}
                    secondary={member.email}
                  />
                </ListItem>
              </Fragment>
            ))}
          </List>
        )}
      </Box>

      <Fab
        color="primary"
        sx={{
          position: 'fixed',
          bottom: 16,
          right: 16,
        }}
        onClick={() => navigate(`/group/${id}/add-expense`)}
      >
        <Add />
      </Fab>
    </Layout>
  );
}
