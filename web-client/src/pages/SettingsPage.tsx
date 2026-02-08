import { useState } from 'react';
import {
  Box,
  TextField,
  Button,
  Typography,
} from '@mui/material';
import { Layout } from '../components/Layout';
import { useToast } from '../hooks/useToast';
import { updateBaseURL } from '../api/client';

export function SettingsPage() {
  const { showSuccess } = useToast();
  const [serverUrl, setServerUrl] = useState(
    localStorage.getItem('serverUrl') || 'http://localhost:9090'
  );

  const handleSave = () => {
    localStorage.setItem('serverUrl', serverUrl);
    updateBaseURL(serverUrl);
    showSuccess('Server URL saved successfully!');
  };

  return (
    <Layout title="Settings" showBack>
      <Box>
        <Typography variant="h6" gutterBottom>
          Server Configuration
        </Typography>

        <TextField
          fullWidth
          label="Server URL"
          value={serverUrl}
          onChange={(e) => setServerUrl(e.target.value)}
          variant="outlined"
          sx={{ mb: 1 }}
        />

        <Typography variant="caption" color="text.secondary" sx={{ mb: 3, display: 'block' }}>
          The URL of the SplitEasy backend server
        </Typography>

        <Button
          fullWidth
          variant="contained"
          onClick={handleSave}
          sx={{ height: 48 }}
        >
          Save
        </Button>
      </Box>
    </Layout>
  );
}
