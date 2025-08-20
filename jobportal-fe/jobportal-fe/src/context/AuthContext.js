import React, { createContext, useContext, useState, useEffect } from 'react';
import { authAPI } from '../services/api';
import toast from 'react-hot-toast';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [roleSelected, setRoleSelected] = useState(false);

  // Token management
  const handleToken = (newToken) => {
    if (newToken) {
      localStorage.setItem('token', newToken);
      setToken(newToken);
    } else {
      localStorage.removeItem('token');
      setToken(null);
    }
  };

  // User data management
  const updateUserData = (userData) => {
    const normalizedUser = {
      email: userData.email || userData.username || 'user@example.com',
      name: userData.name || userData.firstName || userData.email?.split('@')[0] || 'User',
      id: userData.id || userData.userId,
      role: userData.role || 'UNKNOWN',
      role_selected: userData.role_selected !== false,
      provider: userData.provider || 'LOCAL',
      ...userData
    };
    
    localStorage.setItem('user', JSON.stringify(normalizedUser));
    setUser(normalizedUser);
    setRoleSelected(normalizedUser.role_selected);
    return normalizedUser;
  };

  // Handle OAuth Success
  const handleOAuthSuccess = async (token) => {
    try {
      handleToken(token);
      const response = await authAPI.getUserRole();
      const userData = response.data;
      
      const updatedUser = updateUserData({
        ...userData,
        provider: 'GOOGLE'
      });

      if (!updatedUser.role || updatedUser.role === 'UNKNOWN') {
        window.history.replaceState({}, document.title, '/role-selection');
        toast.success('Google login successful! Please select your role.');
      } else {
        window.history.replaceState({}, document.title, '/dashboard');
        toast.success('Google login successful!');
      }
    } catch (error) {
      toast.error('Failed to process OAuth login');
      logout();
    }
  };

  // Regular login
  const login = async (credentials) => {
    try {
      const response = await authAPI.login(credentials);
      const authToken = response.data.token || response.data.access_token;
      
      if (!authToken) throw new Error('No token received');
      
      handleToken(authToken);
      const userResponse = await authAPI.getUserRole();
      
      updateUserData({
        ...userResponse.data,
        email: credentials.email,
        provider: 'LOCAL'
      });

      toast.success('Login successful!');
      return { success: true };
    } catch (error) {
      const message = error.response?.data?.message || 'Login failed';
      toast.error(message);
      return { success: false, error: message };
    }
  };

  // Role selection
  const selectRole = async (role) => {
    try {
      const response = await authAPI.selectRole(role);
      const { token: newToken, user: userData } = response.data;
      
      if (newToken) handleToken(newToken);
      
      updateUserData({
        ...user,
        ...userData,
        role,
        role_selected: true
      });

      toast.success('Role selected successfully!');
      return { success: true };
    } catch (error) {
      toast.error('Role selection failed');
      return { success: false, error: error.message };
    }
  };

  // Logout
  const logout = () => {
    handleToken(null);
    setUser(null);
    setRoleSelected(false);
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = '/login';
  };

  // Register
  const register = async (userData) => {
    try {
      const response = await authAPI.register(userData);
      if (response.data) {
        toast.success('Registration successful!');
        return { success: true };
      }
      return { success: false, error: 'Registration failed' };
    } catch (error) {
      const message = error.response?.data?.message || 'Registration failed';
      toast.error(message);
      return { success: false, error: message };
    }
  };

  // Initial auth check
  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('token');
      if (!storedToken) {
        setLoading(false);
        return;
      }

      try {
        handleToken(storedToken);
        const userResponse = await authAPI.getUserRole();
        updateUserData(userResponse.data);
      } catch (error) {
        logout();
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, []);

  // OAuth redirect handler
  useEffect(() => {
    if (window.location.pathname === '/login/oauth2/code/google') {
      window.location.href = '/oauth-success';
    }

    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    if (token) {
      handleOAuthSuccess(token);
    }
  }, []);

  const value = {
    user,
    token,
    loading,
    roleSelected,
    login,
    register,
    googleLogin: authAPI.googleLogin,
    selectRole,
    logout,
    isAuthenticated: !!token,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};