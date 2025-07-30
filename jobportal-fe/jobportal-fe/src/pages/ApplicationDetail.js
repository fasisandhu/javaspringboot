import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { applicationAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const ApplicationDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [isUpdating, setIsUpdating] = useState(false);

  const { data: applicationResponse, isLoading, error } = useQuery({
    queryKey: ['application', id],
    queryFn: () => applicationAPI.getApplication(id),
    enabled: !!id,
  });

  // Extract application data from response
  const application = applicationResponse?.data || applicationResponse;

  const { register, handleSubmit, formState: { errors } } = useForm({
    defaultValues: {
      status: application?.status || 'PENDING',
      notes: application?.notes || '',
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data) => applicationAPI.updateApplication(id, data),
    onSuccess: () => {
      toast.success('Application updated successfully!');
      queryClient.invalidateQueries(['applications']);
      queryClient.invalidateQueries(['application', id]);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to update application');
    },
  });

  const onSubmit = async (data) => {
    if (user?.role !== 'EMPLOYER' && user?.role !== 'ADMIN') {
      toast.error('Only employers can manage applications');
      return;
    }

    setIsUpdating(true);
    try {
      await updateMutation.mutateAsync(data);
    } finally {
      setIsUpdating(false);
    }
  };

  if (isLoading) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12 text-center">
            <div className="spinner-border" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-2">Loading application details...</p>
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
              Error loading application: {error.message}
            </div>
            <button className="btn btn-outline-primary" onClick={() => navigate('/applications')}>
              <i className="bi bi-arrow-left me-2"></i>
              Back to Applications
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!application) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12">
            <div className="alert alert-warning">
              <i className="bi bi-exclamation-triangle me-2"></i>
              Application not found
            </div>
            <button className="btn btn-outline-primary" onClick={() => navigate('/applications')}>
              <i className="bi bi-arrow-left me-2"></i>
              Back to Applications
            </button>
          </div>
        </div>
      </div>
    );
  }

  const getStatusBadge = (status) => {
    const statusMap = {
      'PENDING': 'bg-warning',
      'ACCEPTED': 'bg-success',
      'REJECTED': 'bg-danger',
      'WITHDRAWN': 'bg-secondary',
      'UNDER_REVIEW': 'bg-info'
    };
    
    return (
      <span className={`badge ${statusMap[status] || 'bg-secondary'}`}>
        {status?.replace('_', ' ')}
      </span>
    );
  };

  return (
    <div className="container mt-4">
      <div className="row">
        <div className="col-12">
          <div className="d-flex justify-content-between align-items-center mb-4">
            <div>
              <button 
                className="btn btn-outline-secondary mb-3"
                onClick={() => navigate('/applications')}
              >
                <i className="bi bi-arrow-left me-2"></i>
                Back to Applications
              </button>
              <h1 className="mb-2">Application Details</h1>
            </div>
            {getStatusBadge(application.status)}
          </div>

          <div className="row">
            <div className="col-md-8">
              {/* Application Information */}
              <div className="card mb-4">
                <div className="card-header">
                  <h5 className="mb-0">
                    <i className="bi bi-file-earmark-text me-2"></i>
                    Application Information
                  </h5>
                </div>
                <div className="card-body">
                  <div className="row">
                    <div className="col-md-6">
                      <h6>Job Details</h6>
                      <p><strong>Title:</strong> {application.job?.title}</p>
                      <p><strong>Company:</strong> {application.job?.company}</p>
                      <p><strong>Salary:</strong> ${application.job?.salary?.toLocaleString()}/year</p>
                      <p><strong>Location:</strong> {application.job?.remote ? 'Remote' : 'On-site'}</p>
                    </div>
                    <div className="col-md-6">
                      <h6>Applicant Details</h6>
                      <p><strong>Name:</strong> {application.applicant?.name || 'N/A'}</p>
                      <p><strong>Email:</strong> {application.applicant?.email}</p>
                      <p><strong>Applied:</strong> {new Date(application.appliedAt || application.id).toLocaleDateString()}</p>
                      <p><strong>Status:</strong> {getStatusBadge(application.status)}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Job Description */}
              {application.job?.description && (
                <div className="card mb-4">
                  <div className="card-header">
                    <h5 className="mb-0">
                      <i className="bi bi-file-text me-2"></i>
                      Job Description
                    </h5>
                  </div>
                  <div className="card-body">
                    <p style={{ whiteSpace: 'pre-wrap' }}>{application.job.description}</p>
                  </div>
                </div>
              )}
            </div>

            <div className="col-md-4">
              {/* Status Management */}
              <div className="card">
                <div className="card-header">
                  <h5 className="mb-0">
                    <i className="bi bi-gear me-2"></i>
                    Manage Application
                  </h5>
                </div>
                <div className="card-body">
                  <form onSubmit={handleSubmit(onSubmit)}>
                    <div className="mb-3">
                      <label htmlFor="status" className="form-label">
                        Application Status
                      </label>
                      <select
                        className={`form-select ${errors.status ? 'is-invalid' : ''}`}
                        id="status"
                        {...register('status')}
                      >
                        <option value="PENDING">Pending</option>
                        <option value="UNDER_REVIEW">Under Review</option>
                        <option value="ACCEPTED">Accepted</option>
                        <option value="REJECTED">Rejected</option>
                        <option value="WITHDRAWN">Withdrawn</option>
                      </select>
                      {errors.status && (
                        <div className="invalid-feedback">{errors.status.message}</div>
                      )}
                    </div>

                    <div className="mb-3">
                      <label htmlFor="notes" className="form-label">
                        Notes
                      </label>
                      <textarea
                        className={`form-control ${errors.notes ? 'is-invalid' : ''}`}
                        id="notes"
                        rows="4"
                        {...register('notes')}
                        placeholder="Add notes about this application..."
                      />
                      {errors.notes && (
                        <div className="invalid-feedback">{errors.notes.message}</div>
                      )}
                    </div>

                    <div className="d-grid">
                      <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={isUpdating}
                      >
                        {isUpdating ? (
                          <>
                            <span className="spinner-border spinner-border-sm me-2"></span>
                            Updating...
                          </>
                        ) : (
                          <>
                            <i className="bi bi-check-circle me-2"></i>
                            Update Application
                          </>
                        )}
                      </button>
                    </div>
                  </form>
                </div>
              </div>

              {/* Quick Actions */}
              <div className="card mt-3">
                <div className="card-header">
                  <h6 className="mb-0">Quick Actions</h6>
                </div>
                <div className="card-body">
                  <div className="d-grid gap-2">
                    <button
                      className="btn btn-outline-primary btn-sm"
                      onClick={() => navigate(`/jobs/${application.jobId}`)}
                    >
                      <i className="bi bi-eye me-1"></i>
                      View Job Details
                    </button>
                    <button
                      className="btn btn-outline-info btn-sm"
                      onClick={() => navigate('/applications')}
                    >
                      <i className="bi bi-list me-1"></i>
                      All Applications
                    </button>
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

export default ApplicationDetail; 