import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../services/api';
import toast from 'react-hot-toast';

const Login = () => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const { login, googleLogin } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const result = await login(formData);
      if (result.success) {
        navigate('/dashboard');
      }
    } catch (error) {
      toast.error('Login failed. Please check your credentials.');
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

  return (
    <div className="min-vh-100 d-flex align-items-center" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
      <div className="container">
        <div className="row justify-content-center">
          <div className="col-md-6 col-lg-4">
            <div className="card shadow-lg border-0">
              <div className="card-body p-5">
                <div className="text-center mb-4">
                  <div className="mb-3">
                    <i className="bi bi-briefcase text-primary" style={{ fontSize: '3rem' }}></i>
                  </div>
                  <h2 className="text-dark fw-bold mb-2">
                    Welcome Back
                  </h2>
                  <p className="text-muted mb-0">
                    Sign in to access your job portal account
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="needs-validation" noValidate>
                  <div className="mb-4">
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

                  <div className="mb-4">
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
                        autoComplete="current-password"
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
                    <div className="invalid-feedback">
                      Please enter your password.
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
                          Signing in...
                        </>
                      ) : (
                        <>
                          <i className="bi bi-box-arrow-in-right me-2"></i>
                          Sign In
                        </>
                      )}
                    </button>
                  </div>

                  <div className="text-center mb-4">
                    <div className="position-relative">
                      <hr className="text-muted" />
                      <span className="position-absolute top-50 start-50 translate-middle bg-white px-3 text-muted">
                        or continue with
                      </span>
                    </div>
                  </div>

                  <div className="d-grid mb-4">
                    <button
                      type="button"
                      className="btn btn-outline-dark btn-lg"
                      onClick={googleLogin}
                    >
                      <i className="bi bi-google me-2"></i>
                      Continue with Google
                    </button>
                  </div>

                  <div className="text-center">
                    <p className="mb-0 text-muted">
                      Don't have an account?{' '}
                      <Link to="/register" className="text-decoration-none fw-semibold">
                        Sign up here
                      </Link>
                    </p>
                  </div>
                </form>
              </div>
            </div>

            {/* Features Section */}
            <div className="row mt-4 text-center text-white">
              <div className="col-md-4 mb-3">
                <div className="d-flex flex-column align-items-center">
                  <i className="bi bi-search mb-2" style={{ fontSize: '1.5rem' }}></i>
                  <small>Find Jobs</small>
                </div>
              </div>
              <div className="col-md-4 mb-3">
                <div className="d-flex flex-column align-items-center">
                  <i className="bi bi-briefcase mb-2" style={{ fontSize: '1.5rem' }}></i>
                  <small>Post Jobs</small>
                </div>
              </div>
              <div className="col-md-4 mb-3">
                <div className="d-flex flex-column align-items-center">
                  <i className="bi bi-people mb-2" style={{ fontSize: '1.5rem' }}></i>
                  <small>Connect</small>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login; 