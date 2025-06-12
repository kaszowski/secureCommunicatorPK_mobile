import React, { useState, useEffect } from 'react';
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { Box, CircularProgress, Typography } from '@mui/material';
import { LoginPage, ChatPage } from './components';
import api from './utils/api';

const theme = createTheme({
  palette: {
    mode: 'light', // Główny tryb kolorów (light/dark) - wpływa na wszystkie komponenty
    primary: {
      main: '#66bb6a', // Główny kolor aplikacji zmieniony na łagodniejszy zielony - przyciski, nagłówki, aktywne elementy
    },
    secondary: {
      main: '#dc004e', // Drugorzędny kolor - akcenty, dodatkowe przyciski
    },
  },
});

interface User {
  userId: string;
  username: string;
  email: string;
}

// Protected Route Component
const ProtectedRoute: React.FC<{
  children: React.ReactNode;
  isAuthenticated: boolean;
}> = ({ children, isAuthenticated }) => {
  return isAuthenticated ? <>{children}</> : <Navigate to='/login' replace />;
};

// Public Route Component (redirect to chat if authenticated)
const PublicRoute: React.FC<{
  children: React.ReactNode;
  isAuthenticated: boolean;
}> = ({ children, isAuthenticated }) => {
  return !isAuthenticated ? <>{children}</> : <Navigate to='/chat' replace />;
};

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      // Try to get user keys to verify authentication
      const response = await api.get('/keys');
      if (response.data.keys) {
        setIsAuthenticated(true);
        // Get actual user profile information
        try {
          const profileResponse = await api.get('/profile');
          if (profileResponse.data) {
            const userInfo: User = {
              userId: profileResponse.data.userId,
              username: profileResponse.data.username,
              email: profileResponse.data.email,
            };
            setUser(userInfo);
            localStorage.setItem('userInfo', JSON.stringify(userInfo));
          }
        } catch (profileError) {
          console.error('Error getting user profile:', profileError);
          // Fallback to localStorage if profile endpoint fails
          const userInfo = localStorage.getItem('userInfo');
          if (userInfo) {
            setUser(JSON.parse(userInfo));
          }
        }
      }
    } catch (error) {
      setIsAuthenticated(false);
      setUser(null);
      localStorage.removeItem('userInfo');
    } finally {
      setLoading(false);
    }
  };

  const handleLogin = (userInfo: User) => {
    setIsAuthenticated(true);
    setUser(userInfo);
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
  };

  const handleLogout = async () => {
    try {
      await api.post('/logout');
    } catch (error: any) {
      // Only log non-401 errors, as 401 is expected when token is already expired
      if (error?.response?.status !== 401) {
        console.error('Logout error:', error);
      }
    } finally {
      setIsAuthenticated(false);
      setUser(null);
      localStorage.removeItem('userInfo');
    }
  };

  if (loading) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box
          display='flex'
          justifyContent='center'
          alignItems='center'
          minHeight='100vh'
          flexDirection='column'
        >
          <CircularProgress size={60} sx={{ mb: 2 }} />
          <Typography variant='h6' color='text.secondary'>
            Loading SecureChat...
          </Typography>
        </Box>
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router basename='/sc'>
        <Routes>
          {/* Default route - redirect based on auth status */}
          <Route
            path='/'
            element={
              <Navigate to={isAuthenticated ? '/chat' : '/login'} replace />
            }
          />

          {/* Chat route - only accessible when authenticated */}
          <Route
            path='/chat'
            element={
              <ProtectedRoute isAuthenticated={isAuthenticated}>
                <ChatPage user={user!} onLogout={handleLogout} />
              </ProtectedRoute>
            }
          />

          {/* Login route - only accessible when not authenticated */}
          <Route
            path='/login'
            element={
              <PublicRoute isAuthenticated={isAuthenticated}>
                <LoginPage onLogin={handleLogin} />
              </PublicRoute>
            }
          />
          {/* Catch all route - redirect to home */}
          <Route path='*' element={<Navigate to='/' replace />} />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;
