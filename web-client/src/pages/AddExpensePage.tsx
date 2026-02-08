import React, { useEffect, useState } from 'react';
import {
  Box,
  TextField,
  Button,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { createExpense } from '../api/expenses';
import { getGroup } from '../api/groups';
import type { User } from '../api/groups';
import { useToast } from '../hooks/useToast';
import { useAuth } from '../hooks/useAuth';
import { dollarsToCents } from '../utils/format';

export function AddExpensePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showSuccess } = useToast();
  const { userId } = useAuth();
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [paidByUserId, setPaidByUserId] = useState<number>(userId || 0);
  const [members, setMembers] = useState<User[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingMembers, setLoadingMembers] = useState(true);

  useEffect(() => {
    const loadMembers = async () => {
      if (!id) return;

      try {
        const group = await getGroup(parseInt(id, 10));
        setMembers(group.members);
        if (userId && group.members.find((m) => m.id === userId)) {
          setPaidByUserId(userId);
        } else if (group.members.length > 0) {
          setPaidByUserId(group.members[0].id);
        }
      } catch (err: any) {
        setError('Failed to load group members');
      } finally {
        setLoadingMembers(false);
      }
    };

    loadMembers();
  }, [id, userId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!description.trim()) {
      setError('Description is required');
      return;
    }

    if (!amount || parseFloat(amount) <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    if (!id) return;

    setLoading(true);

    try {
      const amountCents = dollarsToCents(amount);
      await createExpense(parseInt(id, 10), {
        description,
        amountCents,
        paidByUserId,
      });
      showSuccess('Expense added successfully!');
      navigate(`/group/${id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to add expense');
    } finally {
      setLoading(false);
    }
  };

  if (loadingMembers) {
    return (
      <Layout title="Add Expense" showBack>
        <CircularProgress />
      </Layout>
    );
  }

  return (
    <Layout title="Add Expense" showBack>
      <Box component="form" onSubmit={handleSubmit}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TextField
          fullWidth
          label="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          variant="outlined"
          required
          sx={{ mb: 2 }}
        />

        <TextField
          fullWidth
          label="Amount ($)"
          type="number"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          variant="outlined"
          required
          inputProps={{ step: '0.01', min: '0.01' }}
          sx={{ mb: 2 }}
        />

        <FormControl fullWidth sx={{ mb: 3 }}>
          <InputLabel>Paid By</InputLabel>
          <Select
            value={paidByUserId}
            label="Paid By"
            onChange={(e) => setPaidByUserId(e.target.value as number)}
          >
            {members.map((member) => (
              <MenuItem key={member.id} value={member.id}>
                {member.username}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <Button
          fullWidth
          variant="contained"
          type="submit"
          disabled={loading}
          sx={{ height: 48 }}
        >
          {loading ? <CircularProgress size={24} /> : 'Add Expense'}
        </Button>
      </Box>
    </Layout>
  );
}
