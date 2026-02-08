import type { ReactNode } from 'react';
import { Box, Typography, Button } from '@mui/material';

interface EmptyStateProps {
  icon: ReactNode;
  message: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function EmptyState({ icon, message, action }: EmptyStateProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        py: 6,
        px: 3,
      }}
    >
      <Box sx={{ color: 'text.secondary', mb: 2, fontSize: 48 }}>
        {icon}
      </Box>
      <Typography variant="body1" color="text.secondary" gutterBottom>
        {message}
      </Typography>
      {action && (
        <Button
          variant="outlined"
          onClick={action.onClick}
          sx={{ mt: 2 }}
        >
          {action.label}
        </Button>
      )}
    </Box>
  );
}
