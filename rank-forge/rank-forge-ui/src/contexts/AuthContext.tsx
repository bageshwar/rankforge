import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { authApi } from '../services/api';
import type { UserDTO } from '../services/api';

interface AuthContextType {
  user: UserDTO | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: () => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Load user from localStorage on mount
  useEffect(() => {
    const loadUser = async () => {
      const token = localStorage.getItem('authToken');
      const savedUser = localStorage.getItem('authUser');
      
      if (token && savedUser) {
        try {
          const userData = JSON.parse(savedUser);
          setUser(userData);
          
          // Verify token is still valid by fetching current user
          const currentUser = await authApi.getMe();
          setUser(currentUser.user);
          localStorage.setItem('authUser', JSON.stringify(currentUser.user));
        } catch (error) {
          // Token invalid or expired, clear storage
          localStorage.removeItem('authToken');
          localStorage.removeItem('authUser');
          setUser(null);
        }
      }
      setIsLoading(false);
    };

    loadUser();

    // Listen for auth events
    const handleLogout = () => {
      setUser(null);
      localStorage.removeItem('authToken');
      localStorage.removeItem('authUser');
    };

    const handleLogin = async () => {
      const token = localStorage.getItem('authToken');
      if (token && !user) { // Only fetch if we don't already have a user
        try {
          const currentUser = await authApi.getMe();
          setUser(currentUser.user);
          localStorage.setItem('authUser', JSON.stringify(currentUser.user));
        } catch (error) {
          console.error('Failed to load user after login:', error);
        }
      }
    };

    window.addEventListener('auth:logout', handleLogout);
    window.addEventListener('auth:login', handleLogin);
    return () => {
      window.removeEventListener('auth:logout', handleLogout);
      window.removeEventListener('auth:login', handleLogin);
    };
  }, []);

  const login = async () => {
    try {
      const loginUrl = await authApi.getLoginUrl();
      // Redirect to Steam login
      window.location.href = loginUrl;
    } catch (error) {
      console.error('Failed to initiate login:', error);
      throw error;
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUser');
    authApi.logout().catch(console.error);
  };

  const refreshUser = async () => {
    try {
      const currentUser = await authApi.getMe();
      setUser(currentUser.user);
      localStorage.setItem('authUser', JSON.stringify(currentUser.user));
    } catch (error) {
      console.error('Failed to refresh user:', error);
      // If refresh fails, user might be logged out
      setUser(null);
      localStorage.removeItem('authToken');
      localStorage.removeItem('authUser');
    }
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
