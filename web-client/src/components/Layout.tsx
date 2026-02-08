import type { ReactNode } from 'react';
import { AppBar, Toolbar, Typography, IconButton, Container, Box } from '@mui/material';
import { ArrowBack, Settings, Logout } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface LayoutProps {
  title: string;
  showBack?: boolean;
  showSettings?: boolean;
  showLogout?: boolean;
  children: ReactNode;
  rightAction?: ReactNode;
}

export function Layout({
  title,
  showBack = false,
  showSettings = false,
  showLogout = false,
  children,
  rightAction,
}: LayoutProps) {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'background.default' }}>
      <AppBar position="sticky" color="primary">
        <Toolbar>
          {showBack && (
            <IconButton edge="start" color="inherit" onClick={() => navigate(-1)} sx={{ mr: 2 }}>
              <ArrowBack />
            </IconButton>
          )}
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            {title}
          </Typography>
          {rightAction}
          {showSettings && (
            <IconButton color="inherit" onClick={() => navigate('/settings')}>
              <Settings />
            </IconButton>
          )}
          {showLogout && (
            <IconButton color="inherit" onClick={handleLogout}>
              <Logout />
            </IconButton>
          )}
        </Toolbar>
      </AppBar>
      <Container maxWidth="sm" sx={{ py: 3 }}>
        {children}
      </Container>
    </Box>
  );
}
