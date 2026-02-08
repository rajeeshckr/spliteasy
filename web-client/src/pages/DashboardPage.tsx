import { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Alert,
  Button,
  Fab,
  Stack,
} from '@mui/material';
import { Add, Groups as GroupsIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { MoneyText } from '../components/MoneyText';
import { getDashboard } from '../api/dashboard';
import type { DashboardResponse } from '../api/dashboard';

export function DashboardPage() {
  const navigate = useNavigate();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await getDashboard();
      setData(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  return (
    <Layout title="Dashboard" showSettings showLogout>
      {loading && <LoadingSpinner />}

      {error && (
        <Box>
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
          <Button variant="outlined" onClick={loadDashboard}>
            Retry
          </Button>
        </Box>
      )}

      {!loading && !error && data && (
        <Box sx={{ pb: 10 }}>
          <Card
            sx={{
              mb: 3,
              backgroundColor: '#8EF99D',
              borderRadius: 2,
            }}
          >
            <CardContent>
              <Stack direction="row" spacing={2} justifyContent="space-around">
                <Box sx={{ textAlign: 'center', flex: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    You are owed
                  </Typography>
                  <MoneyText
                    cents={data.totalOwed}
                    variant="h6"
                    fontWeight="bold"
                  />
                </Box>
                <Box
                  sx={{
                    width: 1,
                    backgroundColor: 'rgba(0,0,0,0.12)',
                  }}
                />
                <Box sx={{ textAlign: 'center', flex: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    You owe
                  </Typography>
                  <MoneyText
                    cents={-data.totalOwe}
                    variant="h6"
                    fontWeight="bold"
                  />
                </Box>
              </Stack>
            </CardContent>
          </Card>

          <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>
            My Groups
          </Typography>

          {data.groups.length === 0 ? (
            <EmptyState
              icon={<GroupsIcon fontSize="inherit" />}
              message="No groups yet"
            />
          ) : (
            <Stack spacing={2}>
              {data.groups.map((group) => (
                <Card
                  key={group.id}
                  sx={{ cursor: 'pointer', '&:hover': { boxShadow: 4 } }}
                  onClick={() => navigate(`/group/${group.id}`)}
                >
                  <CardContent>
                    <Box
                      sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}
                    >
                      <Typography variant="subtitle1" fontWeight="medium">
                        {group.name}
                      </Typography>
                      <MoneyText
                        cents={group.myBalance}
                        variant="body2"
                        fontWeight="bold"
                      />
                    </Box>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          )}
        </Box>
      )}

      <Fab
        color="primary"
        sx={{
          position: 'fixed',
          bottom: 16,
          right: 16,
        }}
        onClick={() => navigate('/create-group')}
      >
        <Add />
      </Fab>
    </Layout>
  );
}
