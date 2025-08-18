import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { authAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const RoleSelection = () => {
  const [selectedRole, setSelectedRole] = useState('');
  const { selectRole, user } = useAuth();
  const navigate = useNavigate();

  // Fetch available roles
  const { data: rolesResponse, isLoading, error } = useQuery({
    queryKey: ['roles'],
    queryFn: authAPI.getRoles,
  });

  // Process roles data
  const getRoles = () => {
    if (!rolesResponse) return [];
    
    const response = rolesResponse;
    
    // Handle different response formats
    if (Array.isArray(response.data)) {
      return response.data;
    }
    
    if (response.data && response.data.roles && Array.isArray(response.data.roles)) {
      return response.data.roles;
    }
    
    if (response.data && typeof response.data === 'object') {
      const rolesArray = Object.values(response.data);
      if (rolesArray.length > 0) {
        return rolesArray;
      }
    }
    
    // Fallback roles
    return ['EMPLOYER', 'APPLICANT'];
  };

  const roles = getRoles();

  const handleRoleSelect = async () => {
    if (!selectedRole) {
      toast.error('Please select a role');
      return;
    }

    try {
      const result = await selectRole(selectedRole);
      
      if (result.success) {
        navigate('/dashboard');
      }
    } catch (error) {
      toast.error('Failed to select role. Please try again.');
    }
  };

  if (isLoading) {
    return (
      <div className="container mt-5">
        <div className="row justify-content-center">
          <div className="col-md-6">
            <div className="text-center">
              <div className="spinner-border" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
              <p className="mt-3">Loading available roles...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mt-5">
        <div className="row justify-content-center">
          <div className="col-md-6">
            <div className="alert alert-danger">
              <i className="bi bi-exclamation-triangle me-2"></i>
              Error loading roles: {error.message}
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mt-5">
      <div className="row justify-content-center">
        <div className="col-md-6">
          <div className="card">
            <div className="card-header">
              <h4 className="mb-0">
                <i className="bi bi-person-badge me-2"></i>
                Select Your Role
              </h4>
            </div>
            <div className="card-body">
              {/* User Information Display */}
              {user && (
                <div className="alert alert-info mb-4">
                  <div className="d-flex align-items-center">
                    <i className="bi bi-person-circle me-2" style={{ fontSize: '1.5rem' }}></i>
                    <div>
                      <strong>Welcome, {user.name || user.email || 'User'}!</strong>
                      {user.email && <div className="text-muted small">{user.email}</div>}
                      {user.provider && <div className="text-muted small">Signed in with {user.provider}</div>}
                    </div>
                  </div>
                </div>
              )}
              
              <p className="text-muted mb-4">
                Please select your role to continue. This will determine what features are available to you.
              </p>

              <div className="mb-4">
                <label className="form-label">Choose your role:</label>
                <div className="row g-3">
                  {roles.map((role) => (
                    <div key={role} className="col-md-6">
                      <div className="form-check">
                        <input
                          className="form-check-input"
                          type="radio"
                          name="role"
                          id={role}
                          value={role}
                          checked={selectedRole === role}
                          onChange={(e) => setSelectedRole(e.target.value)}
                        />
                        <label className="form-check-label" htmlFor={role}>
                          <strong>{role}</strong>
                          <br />
                          <small className="text-muted">
                            {role === 'EMPLOYER' 
                              ? 'Post jobs and manage applications'
                              : 'Browse and apply for jobs'
                            }
                          </small>
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="d-grid">
                <button
                  className="btn btn-primary btn-lg"
                  onClick={handleRoleSelect}
                  disabled={!selectedRole}
                >
                  <i className="bi bi-check-circle me-2"></i>
                  Continue with {selectedRole || 'Selected Role'}
                </button>
              </div>

              <div className="mt-3 text-center">
                <small className="text-muted">
                  You can change your role later in your profile settings.
                </small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RoleSelection; 