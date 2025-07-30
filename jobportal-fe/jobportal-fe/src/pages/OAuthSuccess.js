import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const OAuthSuccess = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    const handleOAuthSuccess = async () => {
      try {
        // Get token from URL parameters
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        
        if (token) {
          // Store the token
          localStorage.setItem('token', token);
          
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
              navigate('/role-selection');
              toast.success('Google login successful! Please select your role.');
            } else {
              // User already has role selected
              navigate('/dashboard');
              toast.success('Google login successful!');
            }
          } else {
            throw new Error('Failed to get user info');
          }
        } else {
          // No token in URL, redirect to login
          navigate('/login');
          toast.error('OAuth authentication failed. Please try again.');
        }
      } catch (error) {
        toast.error('Failed to process OAuth token. Please try again.');
        navigate('/login');
      }
    };

    handleOAuthSuccess();
  }, [navigate]);

  return (
    <div className="container mt-5">
      <div className="row justify-content-center">
        <div className="col-md-6">
          <div className="card">
            <div className="card-body text-center">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
              <h4 className="mt-3">Processing OAuth Login...</h4>
              <p className="text-muted">Please wait while we complete your authentication.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OAuthSuccess; 