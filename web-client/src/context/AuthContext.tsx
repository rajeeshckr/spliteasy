import { createContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';

export interface AuthState {
  token: string | null;
  userId: number | null;
  username: string | null;
  isAuthenticated: boolean;
}

export interface AuthContextType extends AuthState {
  login: (token: string, userId: number, username: string) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>({
    token: null,
    userId: null,
    username: null,
    isAuthenticated: false,
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    const username = localStorage.getItem('username');

    if (token && userId && username) {
      setAuthState({
        token,
        userId: parseInt(userId, 10),
        username,
        isAuthenticated: true,
      });
    }
  }, []);

  const login = (token: string, userId: number, username: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('userId', userId.toString());
    localStorage.setItem('username', username);
    setAuthState({
      token,
      userId,
      username,
      isAuthenticated: true,
    });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    setAuthState({
      token: null,
      userId: null,
      username: null,
      isAuthenticated: false,
    });
  };

  return (
    <AuthContext.Provider value={{ ...authState, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
