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

  // Secure token storage in memory only
  const getStoredToken = () => {
    // Only use localStorage for initial load, then keep in memory
    return localStorage.getItem('token');
  };

  const setStoredToken = (newToken) => {
    if (newToken) {
      localStorage.setItem('token', newToken);
    } else {
      localStorage.removeItem('token');
    }
    setToken(newToken);
  };

  // Check if user needs role selection (for Google OAuth users)
  useEffect(() => {
    const checkAuthStatus = async () => {
      const storedToken = getStoredToken();
      const storedUser = localStorage.getItem('user');
      const oauthData = localStorage.getItem('oauth_data');

      // Check if we have OAuth data from the callback page
      if (oauthData) {
        try {
          const data = JSON.parse(oauthData);
          
          const token = data.access_token;
          const roleSelected = data.role_selected;
          
          if (token) {
            setStoredToken(token);
            
            if (roleSelected === false) {
              // User needs to select role
              setRoleSelected(false);
              setUser({ email: 'user@example.com' }); // We'll get the actual email later
              toast.success('Google login successful! Please select your role.');
            } else {
              // User already has role selected
              setRoleSelected(true);
              setUser({ email: 'user@example.com' });
              toast.success('Google login successful!');
            }
            
            // Clear the OAuth data
            localStorage.removeItem('oauth_data');
          }
        } catch (error) {
          localStorage.removeItem('oauth_data');
        }
      } else if (storedToken && storedUser) {
        try {
          // Check if user has selected a role
          const roleResponse = await authAPI.getUserRole();
          const userData = JSON.parse(storedUser);
          
          setUser(userData);
          setStoredToken(storedToken);
          setRoleSelected(true);
        } catch (error) {
          // If role not selected, check if token has role_selected: false
          if (error.response?.status === 401) {
            // Token might be expired or invalid
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            setStoredToken(null);
            setUser(null);
          } else {
            // User needs to select role
            setRoleSelected(false);
          }
        }
      }
      setLoading(false);
    };

    checkAuthStatus();
  }, []);

  // Handle Google OAuth callback
  useEffect(() => {
    // Check if we're on the OAuth callback URL
    if (window.location.pathname === '/login/oauth2/code/google') {
      // Since the OAuth callback is happening on the backend server,
      // we need to handle this differently
      
      // For now, let's redirect back to our app and let the user manually handle the token
      window.location.href = '/oauth-success';
    }
  }, []);

  // Handle OAuth redirects with token
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    
    if (token) {
      const handleOAuthToken = async () => {
        try {
          // Store the token securely
          setStoredToken(token);
          
          // Try to get user info
          const response = await fetch('http://localhost:8080/api/auth/user-role', {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json',
            },
          });
          
          if (response.ok) {
            const userData = await response.json();
            
            if (userData.role_selected === false) {
              // User needs to select role
              setRoleSelected(false);
              setUser({ email: userData.email || 'user@example.com' });
              window.history.replaceState({}, document.title, '/role-selection');
              toast.success('Google login successful! Please select your role.');
            } else {
              // User already has role selected
              setRoleSelected(true);
              setUser({ email: userData.email || 'user@example.com', role: userData.role });
              window.history.replaceState({}, document.title, '/dashboard');
              toast.success('Google login successful!');
            }
          } else {
            throw new Error('Failed to get user info');
          }
        } catch (error) {
          toast.error('Failed to process OAuth token. Please try again.');
          window.history.replaceState({}, document.title, '/login');
        }
      };
      
      handleOAuthToken();
    }
  }, []);

  const login = async (credentials) => {
    try {
      const response = await authAPI.login(credentials);
      
      // Check if response contains login form (failed login)
      if (typeof response.data === 'string' && response.data.includes('Please sign in')) {
        throw new Error('Invalid credentials');
      }
      
      // Check if response contains error messages
      if (typeof response.data === 'string' && response.data.includes('error')) {
        throw new Error('Login failed');
      }
      
      // Store the correct JWT token securely
      const authToken = response.data.token || response.data.access_token;
      if (!authToken) {
        throw new Error('No token received from server');
      }
      setStoredToken(authToken);
      
      // If we get here, login was likely successful
      try {
        const userResponse = await authAPI.getUserRole();
        
        // Handle different user data formats
        let userData;
        if (typeof userResponse.data === 'object') {
          userData = {
            email: credentials.email,
            role: userResponse.data.role || userResponse.data,
            role_selected: userResponse.data.role_selected
          };
        } else {
          userData = {
            email: credentials.email,
            role: userResponse.data,
            role_selected: true
          };
        }
        
        // Store session-based auth info
        localStorage.setItem('isAuthenticated', 'true');
        localStorage.setItem('user', JSON.stringify(userData));
        
        setUser(userData);
        setRoleSelected(true);
        
        toast.success('Login successful!');
        return { success: true };
      } catch (userError) {
        // Try to access a protected endpoint to verify login
        try {
          await authAPI.getAllJobs(); // This should work if logged in
          const userData = { 
            email: credentials.email,
            role: 'UNKNOWN',
            role_selected: true
          };
          
          localStorage.setItem('isAuthenticated', 'true');
          localStorage.setItem('user', JSON.stringify(userData));
          
          setUser(userData);
          setRoleSelected(true);
          
          toast.success('Login successful!');
          return { success: true };
        } catch (authError) {
          // If we can't access protected endpoints, login failed
          throw new Error('Invalid credentials');
        }
      }
    } catch (error) {
      const message = error.response?.data?.message || error.message || 'Login failed';
      toast.error(message);
      return { success: false, error: message };
    }
  };

  const register = async (userData) => {
    try {
      const response = await authAPI.register(userData);
      
      // Check if registration was successful
      if (response.data && response.data.message) {
        // Registration successful but user needs to login
        return { success: true, message: response.data.message };
      }
      
      // Handle automatic login if token is provided
      const authToken = response.data.token || response.data.access_token;
      const userInfo = response.data.user || response.data;
      
      if (authToken) {
        setStoredToken(authToken);
        localStorage.setItem('user', JSON.stringify(userInfo));
        setUser(userInfo);
        setRoleSelected(true);
        
        toast.success('Registration successful!');
        return { success: true };
      }
      
      // No token provided, user needs to login
      return { success: true, message: 'Registration successful! Please login.' };
    } catch (error) {
      const message = error.response?.data?.message || error.message || 'Registration failed';
      toast.error(message);
      return { success: false, error: message };
    }
  };

  const googleLogin = () => {
    authAPI.googleLogin();
  };

  const selectRole = async (role) => {
    try {
      const response = await authAPI.selectRole(role);
      const { token: newToken, user: userData } = response.data;
      
      setStoredToken(newToken);
      localStorage.setItem('user', JSON.stringify(userData));
      
      setUser(userData);
      setRoleSelected(true);
      
      toast.success('Role selected successfully!');
      return { success: true };
    } catch (error) {
      const message = error.response?.data?.message || 'Role selection failed';
      toast.error(message);
      return { success: false, error: message };
    }
  };

  // Enhanced secure logout function
  const logout = async () => {
    try {
      // Clear all authentication data
      setStoredToken(null);
      setUser(null);
      setRoleSelected(false);
      
      // Clear all localStorage items
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      localStorage.removeItem('isAuthenticated');
      localStorage.removeItem('oauth_data');
      
      // Clear any session storage
      sessionStorage.clear();
      
      toast.success('Logged out successfully!');
      
      // Force page reload to clear any cached data
      window.location.href = '/login';
      
    } catch (error) {
      // Even if logout fails, clear local data
      setStoredToken(null);
      setUser(null);
      setRoleSelected(false);
      localStorage.clear();
      sessionStorage.clear();
      window.location.href = '/login';
    }
  };

  const value = {
    user,
    token,
    loading,
    roleSelected,
    login,
    register,
    googleLogin,
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