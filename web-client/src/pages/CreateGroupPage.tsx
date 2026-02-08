import React, { useState } from 'react';
import {
  Box,
  TextField,
  Button,
  Alert,
  CircularProgress,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { createGroup } from '../api/groups';

export function CreateGroupPage() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!name.trim()) {
      setError('Group name is required');
      return;
    }

    setLoading(true);

    try {
      const group = await createGroup({ name, description: description || undefined });
      navigate(`/group/${group.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create group');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout title="Create Group" showBack>
      <Box component="form" onSubmit={handleSubmit}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TextField
          fullWidth
          label="Group Name"
          placeholder="Weekend Trip"
          value={name}
          onChange={(e) => setName(e.target.value)}
          variant="outlined"
          required
          sx={{ mb: 2 }}
        />

        <TextField
          fullWidth
          label="Description"
          placeholder="Mountain hiking trip"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          variant="outlined"
          multiline
          rows={3}
          sx={{ mb: 3 }}
        />

        <Button
          fullWidth
          variant="contained"
          type="submit"
          disabled={loading}
          sx={{ height: 48 }}
        >
          {loading ? <CircularProgress size={24} /> : 'Create Group'}
        </Button>
      </Box>
    </Layout>
  );
}
