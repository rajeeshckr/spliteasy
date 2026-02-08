import { useState, useEffect } from 'react';
import {
  Box,
  TextField,
  Alert,
  List,
  ListItem,
  ListItemText,
  ListItemButton,
  InputAdornment,
  CircularProgress,
  Typography,
} from '@mui/material';
import { Search } from '@mui/icons-material';
import { useParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { searchUsers } from '../api/users';
import { addMember } from '../api/groups';
import type { User } from '../api/groups';
import { useToast } from '../hooks/useToast';

export function AddMemberPage() {
  const { id } = useParams<{ id: string }>();
  const { showSuccess, showError } = useToast();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const timer = setTimeout(async () => {
      if (query.trim().length < 2) {
        setResults([]);
        return;
      }

      setLoading(true);
      setError('');

      try {
        const users = await searchUsers(query);
        setResults(users);
      } catch (err: any) {
        setError('Failed to search users');
      } finally {
        setLoading(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [query]);

  const handleAddMember = async (userId: number, username: string) => {
    if (!id) return;

    try {
      await addMember(parseInt(id, 10), { userId });
      showSuccess(`${username} added to group!`);
      setQuery('');
      setResults([]);
    } catch (err: any) {
      showError(err.response?.data?.message || 'Failed to add member');
    }
  };

  return (
    <Layout title="Add Member" showBack>
      <Box>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <TextField
          fullWidth
          label="Search users"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          variant="outlined"
          placeholder="Enter username or email"
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search />
              </InputAdornment>
            ),
            endAdornment: loading ? (
              <InputAdornment position="end">
                <CircularProgress size={20} />
              </InputAdornment>
            ) : null,
          }}
          sx={{ mb: 2 }}
        />

        {query.trim().length > 0 && query.trim().length < 2 && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Type at least 2 characters to search
          </Typography>
        )}

        {results.length > 0 && (
          <List>
            {results.map((user) => (
              <ListItem key={user.id} disablePadding>
                <ListItemButton onClick={() => handleAddMember(user.id, user.username)}>
                  <ListItemText
                    primary={user.username}
                    secondary={user.email}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        )}

        {!loading && query.trim().length >= 2 && results.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
            No users found
          </Typography>
        )}
      </Box>
    </Layout>
  );
}
