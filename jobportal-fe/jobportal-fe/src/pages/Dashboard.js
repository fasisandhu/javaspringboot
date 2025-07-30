import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { jobAPI, applicationAPI } from '../services/api';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Fetch data based on user role
  const { data: jobsResponse } = useQuery({
    queryKey: ['jobs'],
    queryFn: jobAPI.getAllJobs,
    enabled: !!user,
  });

  const { data: applicationsResponse } = useQuery({
    queryKey: ['applications', user?.role],
    queryFn: () => {
      if (user?.role === 'EMPLOYER') {
        return applicationAPI.getRecruiterApplications();
      } else if (user?.role === 'APPLICANT') {
        return applicationAPI.getMyApplications();
      }
      return Promise.resolve([]);
    },
    enabled: !!user,
  });

  const jobs = jobsResponse?.data || jobsResponse || [];
  const applications = applicationsResponse?.data || applicationsResponse || [];

  const getStatusCount = (status) => {
    return applications.filter(app => app.status === status).length;
  };

  const getRecentJobs = () => {
    return jobs.slice(0, 3);
  };

  if (!user) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12">
            <div className="alert alert-info">
              <i className="bi bi-info-circle me-2"></i>
              Please log in to view your dashboard.
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mt-4">
      <div className="row">
        <div className="col-12">
          {/* Header Section */}
          <div className="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h1 className="mb-2 fw-bold text-dark">
                <i className="bi bi-speedometer2 me-3 text-primary"></i>
                Welcome back, {user.name || user.email}!
              </h1>
              <p className="text-muted mb-0">
                Here's what's happening with your {user.role === 'EMPLOYER' ? 'job postings' : 'job search'} today.
              </p>
            </div>
            <div className="d-flex gap-2">
              <button 
                className="btn btn-outline-primary"
                onClick={() => navigate('/jobs')}
              >
                <i className="bi bi-search me-2"></i>
                Browse Jobs
              </button>
              <button 
                className="btn btn-outline-danger"
                onClick={() => {
                  if (window.confirm('Are you sure you want to logout?')) {
                    logout();
                  }
                }}
                title="Logout (Ctrl+L)"
              >
                <i className="bi bi-box-arrow-right me-2"></i>
                Logout
              </button>
            </div>
          </div>

          {/* Role-specific Dashboard Cards */}
          {user.role === 'EMPLOYER' && (
            <div className="row mb-4">
              <div className="col-md-4 mb-3">
                <div className="card border-0 shadow-sm h-100" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
                  <div className="card-body text-white">
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <h6 className="card-title mb-2">
                          <i className="bi bi-briefcase me-2"></i>
                          My Jobs
                        </h6>
                        <h2 className="mb-0 fw-bold">{jobs.filter(job => job.employerId === user.id).length}</h2>
                        <small className="opacity-75">Active job postings</small>
                      </div>
                      <div className="text-end">
                        <i className="bi bi-briefcase" style={{ fontSize: '2.5rem', opacity: 0.7 }}></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="col-md-4 mb-3">
                <div className="card border-0 shadow-sm h-100" style={{ background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' }}>
                  <div className="card-body text-white">
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <h6 className="card-title mb-2">
                          <i className="bi bi-file-earmark-text me-2"></i>
                          Applications
                        </h6>
                        <h2 className="mb-0 fw-bold">{applications.length}</h2>
                        <small className="opacity-75">Total applications received</small>
                      </div>
                      <div className="text-end">
                        <i className="bi bi-file-earmark-text" style={{ fontSize: '2.5rem', opacity: 0.7 }}></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="col-md-4 mb-3">
                <div className="card border-0 shadow-sm h-100" style={{ background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' }}>
                  <div className="card-body text-white">
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <h6 className="card-title mb-2">
                          <i className="bi bi-clock me-2"></i>
                          Pending
                        </h6>
                        <h2 className="mb-0 fw-bold">{getStatusCount('PENDING')}</h2>
                        <small className="opacity-75">Applications pending review</small>
                      </div>
                      <div className="text-end">
                        <i className="bi bi-clock" style={{ fontSize: '2.5rem', opacity: 0.7 }}></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {user.role === 'APPLICANT' && (
            <div className="row mb-4">
              <div className="col-md-4 mb-3">
                <div className="card border-0 shadow-sm h-100" style={{ background: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)' }}>
                  <div className="card-body">
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <h6 className="card-title mb-2 text-dark">
                          <i className="bi bi-search me-2"></i>
                          Available Jobs
                        </h6>
                        <h2 className="mb-0 fw-bold text-dark">{jobs.length}</h2>
                        <small className="text-muted">Jobs available</small>
                      </div>
                      <div className="text-end">
                        <i className="bi bi-search text-primary" style={{ fontSize: '2.5rem', opacity: 0.7 }}></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="col-md-4 mb-3">
                <div className="card border-0 shadow-sm h-100" style={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
                  <div className="card-body text-white">
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <h6 className="card-title mb-2">
                          <i className="bi bi-file-earmark-text me-2"></i>
                          My Applications
                        </h6>
                        <h2 className="mb-0 fw-bold">{applications.length}</h2>
                        <small className="opacity-75">Applications submitted</small>
                      </div>
                      <div className="text-end">
                        <i className="bi bi-file-earmark-text" style={{ fontSize: '2.5rem', opacity: 0.7 }}></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="col-md-4 mb-3">
                <div className="card border-0 shadow-sm h-100" style={{ background: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' }}>
                  <div className="card-body text-white">
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <h6 className="card-title mb-2">
                          <i className="bi bi-check-circle me-2"></i>
                          Accepted
                        </h6>
                        <h2 className="mb-0 fw-bold">{getStatusCount('ACCEPTED')}</h2>
                        <small className="opacity-75">Applications accepted</small>
                      </div>
                      <div className="text-end">
                        <i className="bi bi-check-circle" style={{ fontSize: '2.5rem', opacity: 0.7 }}></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Quick Actions */}
          <div className="row mb-4">
            <div className="col-12">
              <div className="card border-0 shadow-sm">
                <div className="card-header bg-transparent border-0">
                  <h5 className="mb-0 fw-bold text-dark">
                    <i className="bi bi-lightning me-2 text-warning"></i>
                    Quick Actions
                  </h5>
                </div>
                <div className="card-body">
                  <div className="row">
                    {user.role === 'EMPLOYER' && (
                      <>
                        <div className="col-md-4 mb-3">
                          <button 
                            className="btn btn-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-3"
                            onClick={() => navigate('/jobs/create')}
                            style={{ minHeight: '100px' }}
                          >
                            <i className="bi bi-plus-circle mb-2" style={{ fontSize: '1.5rem' }}></i>
                            <span className="fw-semibold">Post New Job</span>
                          </button>
                        </div>
                        <div className="col-md-4 mb-3">
                          <button 
                            className="btn btn-outline-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-3"
                            onClick={() => navigate('/jobs')}
                            style={{ minHeight: '100px' }}
                          >
                            <i className="bi bi-briefcase mb-2" style={{ fontSize: '1.5rem' }}></i>
                            <span className="fw-semibold">Manage Jobs</span>
                          </button>
                        </div>
                        <div className="col-md-4 mb-3">
                          <button 
                            className="btn btn-outline-info w-100 h-100 d-flex flex-column align-items-center justify-content-center p-3"
                            onClick={() => navigate('/applications')}
                            style={{ minHeight: '100px' }}
                          >
                            <i className="bi bi-file-earmark-text mb-2" style={{ fontSize: '1.5rem' }}></i>
                            <span className="fw-semibold">View Applications</span>
                          </button>
                        </div>
                      </>
                    )}
                    {user.role === 'APPLICANT' && (
                      <>
                        <div className="col-md-6 mb-3">
                          <button 
                            className="btn btn-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-3"
                            onClick={() => navigate('/jobs')}
                            style={{ minHeight: '100px' }}
                          >
                            <i className="bi bi-search mb-2" style={{ fontSize: '1.5rem' }}></i>
                            <span className="fw-semibold">Browse Jobs</span>
                          </button>
                        </div>
                        <div className="col-md-6 mb-3">
                          <button 
                            className="btn btn-outline-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-3"
                            onClick={() => navigate('/applications')}
                            style={{ minHeight: '100px' }}
                          >
                            <i className="bi bi-file-earmark-text mb-2" style={{ fontSize: '1.5rem' }}></i>
                            <span className="fw-semibold">My Applications</span>
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Recent Jobs Section */}
          <div className="row">
            <div className="col-12">
              <div className="card border-0 shadow-sm">
                <div className="card-header bg-transparent border-0">
                  <h5 className="mb-0 fw-bold text-dark">
                    <i className="bi bi-briefcase me-2 text-primary"></i>
                    Recent Jobs
                  </h5>
                </div>
                <div className="card-body">
                  {getRecentJobs().length === 0 ? (
                    <div className="text-center py-4">
                      <i className="bi bi-briefcase text-muted" style={{ fontSize: '3rem' }}></i>
                      <p className="text-muted mt-3">No recent jobs available</p>
                    </div>
                  ) : (
                    <div className="row">
                      {getRecentJobs().map((job) => (
                        <div key={job.id} className="col-md-4 mb-3">
                          <div className="card h-100 border-0 shadow-sm">
                            <div className="card-body">
                              <div className="d-flex justify-content-between align-items-start mb-2">
                                <h6 className="card-title mb-0 fw-bold">{job.title}</h6>
                                <span className="badge bg-primary">{job.remote ? 'Remote' : 'On-site'}</span>
                              </div>
                              <p className="text-muted mb-2">
                                <i className="bi bi-building me-1"></i>
                                {job.company}
                              </p>
                              <div className="d-flex justify-content-between align-items-center">
                                <span className="text-primary fw-bold">
                                  ${job.salary?.toLocaleString()}
                                </span>
                                <button 
                                  className="btn btn-sm btn-outline-primary"
                                  onClick={() => navigate(`/jobs/${job.id}`)}
                                >
                                  View Details
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 