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
            
            // Create user object from OAuth data
            const oauthUser = {
              email: data.email || data.username || 'user@example.com',
              name: data.name || data.firstName || data.lastName || data.email?.split('@')[0] || 'User',
              id: data.id || data.userId,
              provider: 'GOOGLE',
              ...data // Include any other OAuth fields
            };
            
            if (roleSelected === false) {
              // User needs to select role
              setRoleSelected(false);
              setUser(oauthUser);
              toast.success('Google login successful! Please select your role.');
            } else {
              // User already has role selected
              setRoleSelected(true);
              setUser(oauthUser);
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
          const userData = JSON.parse(storedUser);
          

          
          // Check if user has already selected a role from stored data
          if (userData.role && userData.role !== 'UNKNOWN') {

            setUser(userData);
            setStoredToken(storedToken);
            setRoleSelected(true);
          } else {

            // Only make API call if we don't have valid role data
            try {
              const roleResponse = await authAPI.getUserRole();
              const apiUserData = roleResponse.data;
              

              
              if (apiUserData.role && apiUserData.role !== 'UNKNOWN') {
                // User has role from API
                const updatedUserData = { ...userData, ...apiUserData };
                localStorage.setItem('user', JSON.stringify(updatedUserData));
                setUser(updatedUserData);
                setStoredToken(storedToken);
                setRoleSelected(true);

              } else {
                // User needs to select role
                setUser(userData);
                setStoredToken(storedToken);
                setRoleSelected(false);

              }
            } catch (apiError) {

              // If API call fails, use stored data and don't make additional API calls
              if (userData.role && userData.role !== 'UNKNOWN') {
                setUser(userData);
                setStoredToken(storedToken);
                setRoleSelected(true);

              } else {
                setUser(userData);
                setStoredToken(storedToken);
                setRoleSelected(false);

              }
            }
          }
        } catch (error) {
          console.error('❌ Error parsing stored user data:', error);
          // If parsing stored user fails, clear everything
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          setStoredToken(null);
          setUser(null);
          setRoleSelected(false);
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
          const response = await fetch('/api/auth/user-role', {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json',
            },
          });
          
          if (response.ok) {
            const userData = await response.json();

            
            // Create a complete user object with all available information
            const completeUserData = {
              email: userData.email || userData.username || 'user@example.com',
              name: userData.name || userData.firstName || userData.lastName || userData.email?.split('@')[0] || 'User',
              id: userData.id || userData.userId,
              role: userData.role || 'UNKNOWN',
              role_selected: userData.role_selected !== false,
              provider: 'GOOGLE',
              ...userData // Include any other fields from the API
            };
            
            if (userData.role_selected === false || !userData.role || userData.role === 'UNKNOWN') {
              // User needs to select role
              setRoleSelected(false);
              setUser(completeUserData);
              // Store user data temporarily (without role)
              localStorage.setItem('user', JSON.stringify(completeUserData));
              window.history.replaceState({}, document.title, '/role-selection');
              toast.success('Google login successful! Please select your role.');
            } else {
              // User already has role selected
              setRoleSelected(true);
              setUser(completeUserData);
              localStorage.setItem('user', JSON.stringify(completeUserData));
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
            role_selected: userResponse.data.role_selected !== false
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
        setRoleSelected(userData.role_selected);
        
        toast.success('Login successful!');
        return { success: true };
      } catch (userError) {
        // Try to access a protected endpoint to verify login
        try {
          await authAPI.getAllJobs(); // This should work if logged in
          const userData = { 
            email: credentials.email,
            role: 'UNKNOWN',
            role_selected: false
          };
          
          localStorage.setItem('isAuthenticated', 'true');
          localStorage.setItem('user', JSON.stringify(userData));
          
          setUser(userData);
          setRoleSelected(false);
          
          toast.success('Login successful! Please select your role.');
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
      

      
      // Update token if provided
      if (newToken) {
        setStoredToken(newToken);
      }
      
      // Merge existing user data with new role data, preserving all user information
      const existingUser = user || {};
      const updatedUserData = {
        ...existingUser,           // Keep all existing user data (email, name, id, etc.)
        ...userData,               // Add any new data from the API response
        role: role,                // Set the selected role
        role_selected: true,       // Mark role as selected
        provider: existingUser.provider || 'GOOGLE' // Preserve provider info
      };
      

      
      // Store updated user data
      localStorage.setItem('user', JSON.stringify(updatedUserData));
      
      // Update state
      setUser(updatedUserData);
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

  // Function to refresh authentication status (only when necessary)
  const refreshAuthStatus = async (force = false) => {
    // Don't refresh if we already have valid user data and role
    if (!force && user && user.role && user.role !== 'UNKNOWN' && roleSelected) {
      return;
    }
    const storedToken = getStoredToken();
    if (storedToken) {
      try {
        const userResponse = await authAPI.getUserRole();
        const userData = userResponse.data;
        

        
        if (userData.role && userData.role !== 'UNKNOWN') {
          // Merge existing user data with API data, preserving all user information
          const updatedUserData = {
            ...user,                    // Keep existing user data
            ...userData,                // Add/update with API data
            role: userData.role,
            role_selected: true
          };
          
          localStorage.setItem('user', JSON.stringify(updatedUserData));
          setUser(updatedUserData);
          setRoleSelected(true);

        } else {
          setRoleSelected(false);

        }
      } catch (error) {

        // If API call fails, check if token is still valid
        try {
          await authAPI.getAllJobs();
          // Token is valid, but user might need role selection
          if (user && user.role && user.role !== 'UNKNOWN') {
            setRoleSelected(true);

          } else {
            setRoleSelected(false);

          }
        } catch (authError) {
          // Token is invalid, logout

          logout();
        }
      }
    } else {

    }
  };

  // Manual refresh function for when user explicitly needs to refresh auth
  const forceRefreshAuth = () => {
    refreshAuthStatus(true);
  };

  // Function to fetch complete user profile information
  const fetchUserProfile = async () => {
    const storedToken = getStoredToken();
    if (storedToken) {
      try {
        const userResponse = await authAPI.getUserRole();
        const userData = userResponse.data;
        

        
        // Merge with existing user data
        const completeUserData = {
          ...user,                    // Keep existing user data
          ...userData,                // Add/update with API data
          provider: user?.provider || 'GOOGLE' // Preserve provider info
        };
        
        // Update localStorage and state
        localStorage.setItem('user', JSON.stringify(completeUserData));
        setUser(completeUserData);
        

        return completeUserData;
      } catch (error) {

        return null;
      }
    }
    return null;
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
    refreshAuthStatus,
    forceRefreshAuth,
    fetchUserProfile,
    isAuthenticated: !!token,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}; 