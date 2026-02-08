import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#006E26',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#006A62',
      contrastText: '#FFFFFF',
    },
    error: {
      main: '#BA1A1A',
    },
    success: {
      main: '#006E26',
    },
    background: {
      default: '#FCFDF7',
      paper: '#FFFFFF',
    },
  },
  typography: {
    fontFamily: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'].join(','),
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 600 },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: { borderRadius: 16 },
      },
    },
  },
});

export default theme;
