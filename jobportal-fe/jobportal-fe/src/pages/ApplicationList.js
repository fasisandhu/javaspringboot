import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { applicationAPI } from '../services/api';

const ApplicationList = () => {
  const { user } = useAuth();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');

  // Fetch applications based on user role
  const { data: applicationsResponse, isLoading, error } = useQuery({
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

  const applications = applicationsResponse?.data || applicationsResponse || [];

  // Filter applications
  const filteredApplications = applications.filter(app => {
    const matchesSearch = 
      app.jobTitle?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      app.companyName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (user?.role === 'EMPLOYER' && app.applicantName?.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (user?.role === 'EMPLOYER' && app.applicantEmail?.toLowerCase().includes(searchTerm.toLowerCase()));
    
    const matchesStatus = filterStatus === 'all' || app.status === filterStatus;
    
    return matchesSearch && matchesStatus;
  });

  const getStatusBadge = (status) => {
    const statusConfig = {
      'PENDING': { color: 'warning', icon: 'clock' },
      'ACCEPTED': { color: 'success', icon: 'check-circle' },
      'REJECTED': { color: 'danger', icon: 'x-circle' },
      'WITHDRAWN': { color: 'secondary', icon: 'arrow-left' }
    };
    
    const config = statusConfig[status] || { color: 'secondary', icon: 'question-circle' };
    
    return (
      <span className={`badge bg-${config.color}`}>
        <i className={`bi bi-${config.icon} me-1`}></i>
        {status}
      </span>
    );
  };

  if (isLoading) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12 text-center">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3">Loading applications...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12">
            <div className="alert alert-danger">
              <i className="bi bi-exclamation-triangle me-2"></i>
              Error loading applications: {error.message}
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
          {/* Header */}
          <div className="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h1 className="mb-2 fw-bold text-dark">
                <i className="bi bi-file-earmark-text me-3 text-primary"></i>
                {user?.role === 'EMPLOYER' ? 'Received Applications' : 'My Applications'}
              </h1>
              <p className="text-muted mb-0">
                {filteredApplications.length} application{filteredApplications.length !== 1 ? 's' : ''} found
              </p>
            </div>
          </div>

          {/* Filters */}
          <div className="card border-0 shadow-sm mb-4">
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-6">
                  <label className="form-label fw-semibold">
                    <i className="bi bi-search me-2"></i>
                    Search Applications
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder={
                      user?.role === 'EMPLOYER' 
                        ? "Search by job title, company, or applicant name..."
                        : "Search by job title or company..."
                    }
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
                <div className="col-md-4">
                  <label className="form-label fw-semibold">
                    <i className="bi bi-funnel me-2"></i>
                    Filter by Status
                  </label>
                  <select
                    className="form-select"
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                  >
                    <option value="all">All Statuses</option>
                    <option value="PENDING">Pending</option>
                    <option value="ACCEPTED">Accepted</option>
                    <option value="REJECTED">Rejected</option>
                    <option value="WITHDRAWN">Withdrawn</option>
                  </select>
                </div>
                <div className="col-md-2 d-flex align-items-end">
                  <button 
                    className="btn btn-outline-secondary w-100"
                    onClick={() => {
                      setSearchTerm('');
                      setFilterStatus('all');
                    }}
                  >
                    <i className="bi bi-arrow-clockwise me-2"></i>
                    Reset
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Applications List */}
          {filteredApplications.length === 0 ? (
            <div className="text-center py-5">
              <i className="bi bi-file-earmark-text text-muted" style={{ fontSize: '4rem' }}></i>
              <h3 className="mt-3 text-muted">No applications found</h3>
              <p className="text-muted">
                {searchTerm || filterStatus !== 'all' 
                  ? 'Try adjusting your search criteria'
                  : user?.role === 'EMPLOYER' 
                    ? 'No applications have been received yet'
                    : 'You haven\'t applied for any jobs yet'
                }
              </p>
              {user?.role === 'APPLICANT' && (
                <a href="/jobs" className="btn btn-primary">
                  <i className="bi bi-search me-2"></i>
                  Browse Available Jobs
                </a>
              )}
            </div>
          ) : (
            <div className="row g-4">
              {filteredApplications.map((application) => (
                <div key={application.applicationId} className="col-12">
                  <div className="card border-0 shadow-sm h-100">
                    <div className="card-body">
                      <div className="row align-items-center">
                        <div className="col-md-8">
                          <div className="d-flex justify-content-between align-items-start mb-3">
                            <div>
                              <h5 className="card-title mb-1 fw-bold text-primary">
                                {application.jobTitle}
                              </h5>
                              <h6 className="card-subtitle mb-2 text-muted">
                                <i className="bi bi-building me-2"></i>
                                {application.companyName}
                              </h6>
                            </div>
                            <div className="text-end">
                              {getStatusBadge(application.status)}
                            </div>
                          </div>
                          
                          {user?.role === 'EMPLOYER' && (
                            <div className="row mb-3">
                              <div className="col-md-6">
                                <div className="d-flex align-items-center">
                                  <i className="bi bi-person me-2 text-muted"></i>
                                  <span className="text-muted">
                                    <strong>Applicant:</strong> {application.applicantName}
                                  </span>
                                </div>
                              </div>
                              <div className="col-md-6">
                                <div className="d-flex align-items-center">
                                  <i className="bi bi-envelope me-2 text-muted"></i>
                                  <span className="text-muted">
                                    <strong>Email:</strong> {application.applicantEmail}
                                  </span>
                                </div>
                              </div>
                            </div>
                          )}
                          
                          <div className="d-flex align-items-center text-muted">
                            <i className="bi bi-calendar me-2"></i>
                            <small>
                              Applied on {new Date(application.applicationId).toLocaleDateString()}
                            </small>
                            <span className="mx-2">•</span>
                            <small>
                              Application ID: {application.applicationId}
                            </small>
                          </div>
                        </div>
                        
                        <div className="col-md-4 text-end">
                          <button
                            className="btn btn-outline-primary"
                            onClick={() => window.open(`/jobs/${application.jobId}`, '_blank')}
                          >
                            <i className="bi bi-eye me-2"></i>
                            View Job Details
                          </button>
                        </div>
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
  );
};

export default ApplicationList; 