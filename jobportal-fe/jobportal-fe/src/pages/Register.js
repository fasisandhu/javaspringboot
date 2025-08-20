import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const Register = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'APPLICANT',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (formData.password !== formData.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    if (formData.password.length < 6) {
      toast.error('Password must be at least 6 characters long');
      return;
    }

    setIsLoading(true);

    try {
      // Remove confirmPassword from the data sent to API
      const { confirmPassword, ...registrationData } = formData;
      const result = await register(registrationData);
      
      if (result.success) {
        toast.success('Registration successful! Please login.');
        navigate('/login');
      } else {
        toast.error(result.error || 'Registration failed');
      }
    } catch (error) {
      toast.error('Registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  const toggleConfirmPasswordVisibility = () => {
    setShowConfirmPassword(!showConfirmPassword);
  };

  const getPasswordStrength = (password) => {
    if (!password) return { strength: 0, color: '', text: '' };
    
    let score = 0;
    if (password.length >= 6) score++;
    if (password.length >= 8) score++;
    if (/[a-z]/.test(password)) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    if (score <= 2) return { strength: score, color: 'danger', text: 'Weak' };
    if (score <= 4) return { strength: score, color: 'warning', text: 'Fair' };
    return { strength: score, color: 'success', text: 'Strong' };
  };

  const passwordStrength = getPasswordStrength(formData.password);

  return (
    <div className="min-vh-100 d-flex align-items-center" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
      <div className="container">
        <div className="row justify-content-center">
          <div className="col-md-8 col-lg-6">
            <div className="card shadow-lg border-0">
              <div className="card-body p-5">
                <div className="text-center mb-4">
                  <div className="mb-3">
                    <i className="bi bi-person-plus text-primary" style={{ fontSize: '3rem' }}></i>
                  </div>
                  <h2 className="text-dark fw-bold mb-2">
                    Create Account
                  </h2>
                  <p className="text-muted mb-0">
                    Join our job portal community and start your journey
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="needs-validation" noValidate>
                  <div className="row">
                    <div className="col-md-6 mb-4">
                      <label htmlFor="name" className="form-label fw-semibold">
                        <i className="bi bi-person me-2"></i>
                        Full Name
                      </label>
                      <input
                        type="text"
                        className="form-control form-control-lg"
                        id="name"
                        name="name"
                        value={formData.name}
                        onChange={handleChange}
                        required
                        placeholder="Enter your full name"
                        autoComplete="name"
                      />
                      <div className="invalid-feedback">
                        Please enter your full name.
                      </div>
                    </div>

                    <div className="col-md-6 mb-4">
                      <label htmlFor="email" className="form-label fw-semibold">
                        <i className="bi bi-envelope me-2"></i>
                        Email Address
                      </label>
                      <input
                        type="email"
                        className="form-control form-control-lg"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        required
                        placeholder="Enter your email"
                        autoComplete="email"
                      />
                      <div className="invalid-feedback">
                        Please enter a valid email address.
                      </div>
                    </div>
                  </div>

                  <div className="mb-4">
                    <label htmlFor="role" className="form-label fw-semibold">
                      <i className="bi bi-person-badge me-2"></i>
                      I want to
                    </label>
                    <select
                      className="form-select form-select-lg"
                      id="role"
                      name="role"
                      value={formData.role}
                      onChange={handleChange}
                      required
                    >
                      <option value="APPLICANT">Find a job</option>
                      <option value="EMPLOYER">Post jobs</option>
                    </select>
                  </div>

                  <div className="row">
                    <div className="col-md-6 mb-4">
                      <label htmlFor="password" className="form-label fw-semibold">
                        <i className="bi bi-lock me-2"></i>
                        Password
                      </label>
                      <div className="input-group input-group-lg">
                        <input
                          type={showPassword ? 'text' : 'password'}
                          className="form-control"
                          id="password"
                          name="password"
                          value={formData.password}
                          onChange={handleChange}
                          required
                          placeholder="Enter your password"
                          autoComplete="new-password"
                        />
                        <button
                          type="button"
                          className="btn btn-outline-secondary"
                          onClick={togglePasswordVisibility}
                          title={showPassword ? 'Hide password' : 'Show password'}
                        >
                          <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`}></i>
                        </button>
                      </div>
                      {formData.password && (
                        <div className="mt-2">
                          <div className="progress" style={{ height: '4px' }}>
                            <div 
                              className={`progress-bar bg-${passwordStrength.color}`}
                              style={{ width: `${(passwordStrength.strength / 6) * 100}%` }}
                            ></div>
                          </div>
                          <small className={`text-${passwordStrength.color} fw-semibold`}>
                            {passwordStrength.text} password
                          </small>
                        </div>
                      )}
                      <div className="invalid-feedback">
                        Please enter a password.
                      </div>
                    </div>

                    <div className="col-md-6 mb-4">
                      <label htmlFor="confirmPassword" className="form-label fw-semibold">
                        <i className="bi bi-lock-fill me-2"></i>
                        Confirm Password
                      </label>
                      <div className="input-group input-group-lg">
                        <input
                          type={showConfirmPassword ? 'text' : 'password'}
                          className="form-control"
                          id="confirmPassword"
                          name="confirmPassword"
                          value={formData.confirmPassword}
                          onChange={handleChange}
                          required
                          placeholder="Confirm your password"
                          autoComplete="new-password"
                        />
                        <button
                          type="button"
                          className="btn btn-outline-secondary"
                          onClick={toggleConfirmPasswordVisibility}
                          title={showConfirmPassword ? 'Hide password' : 'Show password'}
                        >
                          <i className={`bi ${showConfirmPassword ? 'bi-eye-slash' : 'bi-eye'}`}></i>
                        </button>
                      </div>
                      {formData.confirmPassword && formData.password !== formData.confirmPassword && (
                        <small className="text-danger fw-semibold">
                          Passwords do not match
                        </small>
                      )}
                      <div className="invalid-feedback">
                        Please confirm your password.
                      </div>
                    </div>
                  </div>

                  <div className="d-grid mb-4">
                    <button
                      type="submit"
                      className="btn btn-primary btn-lg fw-semibold"
                      disabled={isLoading}
                    >
                      {isLoading ? (
                        <>
                          <span className="spinner-border spinner-border-sm me-2"></span>
                          Creating account...
                        </>
                      ) : (
                        <>
                          <i className="bi bi-person-plus me-2"></i>
                          Create Account
                        </>
                      )}
                    </button>
                  </div>

                  <div className="text-center">
                    <p className="mb-0 text-muted">
                      Already have an account?{' '}
                      <Link to="/login" className="text-decoration-none fw-semibold">
                        Sign in here
                      </Link>
                    </p>
                  </div>
                </form>
              </div>
            </div>

            {/* Features Section */}
            <div className="row mt-4 text-center text-white">
              <div className="col-md-3 mb-3">
                <div className="d-flex flex-column align-items-center">
                  <i className="bi bi-search mb-2" style={{ fontSize: '1.5rem' }}></i>
                  <small>Find Jobs</small>
                </div>
              </div>
              <div className="col-md-3 mb-3">
                <div className="d-flex flex-column align-items-center">
                  <i className="bi bi-briefcase mb-2" style={{ fontSize: '1.5rem' }}></i>
                  <small>Post Jobs</small>
                </div>
              </div>
              <div className="col-md-3 mb-3">
                <div className="d-flex flex-column align-items-center">
                  <i className="bi bi-people mb-2" style={{ fontSize: '1.5rem' }}></i>
                  <small>Connect</small>
                </div>
              </div>
              <div className="col-md-3 mb-3">
                <div className="d-flex flex-column align-items-center">
                  <i className="bi bi-shield-check mb-2" style={{ fontSize: '1.5rem' }}></i>
                  <small>Secure</small>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;