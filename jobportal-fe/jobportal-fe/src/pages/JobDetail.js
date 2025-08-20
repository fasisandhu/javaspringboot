import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobAPI, applicationAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const JobDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const { data: jobResponse, isLoading, error } = useQuery({
    queryKey: ['job', id],
    queryFn: () => jobAPI.getJob(id),
    enabled: !!id,
  });

  // Extract job data from response
  const job = jobResponse?.data || jobResponse;

  const applyMutation = useMutation({
    mutationFn: () => applicationAPI.applyForJob(id),
    onSuccess: () => {
      toast.success('Application submitted successfully!');
      queryClient.invalidateQueries(['applications']);
    },
    onError: (error) => {
      if (error.response?.status === 409) {
        toast.error('You have already applied for this job');
      } else {
        toast.error(error.response?.data?.message || 'Failed to apply for job');
      }
    },
  });

  const handleApply = async () => {
    if (!user) {
      toast.error('Please log in to apply for jobs');
      return;
    }
    
    if (user.role !== 'APPLICANT') {
      toast.error('Only applicants can apply for jobs');
      return;
    }

    applyMutation.mutate();
  };

  if (isLoading) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12 text-center">
            <div className="spinner-border" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-2">Loading job details...</p>
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
              Error loading job: {error.message}
            </div>
            <button className="btn btn-outline-primary" onClick={() => navigate('/jobs')}>
              <i className="bi bi-arrow-left me-2"></i>
              Back to Jobs
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12">
            <div className="alert alert-warning">
              <i className="bi bi-exclamation-triangle me-2"></i>
              Job not found
            </div>
            <button className="btn btn-outline-primary" onClick={() => navigate('/jobs')}>
              <i className="bi bi-arrow-left me-2"></i>
              Back to Jobs
            </button>
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
          <div className="d-flex justify-content-between align-items-start mb-4">
            <div>
              <button 
                className="btn btn-outline-secondary mb-3"
                onClick={() => navigate('/jobs')}
              >
                <i className="bi bi-arrow-left me-2"></i>
                Back to Jobs
              </button>
              <h1 className="mb-2">{job.title}</h1>
              <h4 className="text-muted mb-0">
                <i className="bi bi-building me-2"></i>
                {job.company}
              </h4>
            </div>
            <div className="d-flex gap-2">
              {user?.role === 'EMPLOYER' && (
                <button 
                  className="btn btn-outline-primary"
                  onClick={() => {
                    if (job.employerId !== user.id) {
                      toast.error('You are not authorized to edit this job');
                      return;
                    }
                    navigate(`/jobs/${id}/edit`);
                  }}
                >
                  <i className="bi bi-pencil me-2"></i>
                  Edit
                </button>
              )}
              {user?.role === 'APPLICANT' && (
                <button 
                  className="btn btn-success"
                  onClick={handleApply}
                  disabled={applyMutation.isPending}
                >
                  {applyMutation.isPending ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2"></span>
                      Applying...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-send me-2"></i>
                      Apply Now
                    </>
                  )}
                </button>
              )}
            </div>
          </div>

          {/* Job Details Card */}
          <div className="card">
            <div className="card-body">
              <div className="row">
                <div className="col-md-8">
                  {/* Job Description */}
                  <div className="mb-4">
                    <h5 className="card-title">
                      <i className="bi bi-file-text me-2"></i>
                      Job Description
                    </h5>
                    <p className="card-text" style={{ whiteSpace: 'pre-wrap' }}>
                      {job.description}
                    </p>
                  </div>
                </div>

                <div className="col-md-4">
                  {/* Job Info Sidebar */}
                  <div className="card bg-light">
                    <div className="card-body">
                      <h6 className="card-title mb-3">Job Information</h6>
                      
                      {/* Salary */}
                      <div className="mb-3">
                        <small className="text-muted">Salary</small>
                        <div className="fw-bold text-primary">
                          ${job.salary?.toLocaleString()}/year
                        </div>
                      </div>

                      {/* Location */}
                      <div className="mb-3">
                        <small className="text-muted">Location</small>
                        <div className="d-flex align-items-center">
                          {job.remote ? (
                            <>
                              <span className="badge bg-success me-2">
                                <i className="bi bi-wifi"></i>
                              </span>
                              Remote
                            </>
                          ) : (
                            <>
                              <i className="bi bi-geo-alt me-2"></i>
                              On-site
                            </>
                          )}
                        </div>
                      </div>

                      {/* Posted By */}
                      <div className="mb-3">
                        <small className="text-muted">Posted By</small>
                        <div>
                          <i className="bi bi-person me-2"></i>
                          {job.postedBy}
                        </div>
                      </div>

                      {/* Job ID */}
                      <div className="mb-3">
                        <small className="text-muted">Job ID</small>
                        <div>
                          #{job.id}
                        </div>
                      </div>

                      {/* Apply Button for Applicants */}
                      {user?.role === 'APPLICANT' && (
                        <div className="mt-4">
                          <button 
                            className="btn btn-success w-100"
                            onClick={handleApply}
                            disabled={applyMutation.isPending}
                          >
                            {applyMutation.isPending ? (
                              <>
                                <span className="spinner-border spinner-border-sm me-2"></span>
                                Applying...
                              </>
                            ) : (
                              <>
                                <i className="bi bi-send me-2"></i>
                                Apply Now
                              </>
                            )}
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default JobDetail;