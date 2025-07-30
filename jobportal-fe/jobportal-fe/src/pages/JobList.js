import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobAPI, applicationAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const JobList = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [expandedJobs, setExpandedJobs] = useState(new Set());
  const [searchTerm, setSearchTerm] = useState('');
  const [filterRemote, setFilterRemote] = useState('all');
  const [sortBy, setSortBy] = useState('newest');

  // Fetch jobs
  const { data: jobsResponse, isLoading, error } = useQuery({
    queryKey: ['jobs'],
    queryFn: jobAPI.getAllJobs,
  });

  const jobs = jobsResponse?.data || jobsResponse || [];

  // Apply mutations
  const applyMutation = useMutation({
    mutationFn: (jobId) => applicationAPI.applyForJob(jobId),
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

  const deleteMutation = useMutation({
    mutationFn: (job) => jobAPI.deleteJob(job.id, job),
    onSuccess: () => {
      toast.success('Job deleted successfully!');
      queryClient.invalidateQueries(['jobs']);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to delete job');
    },
  });

  const toggleJobExpansion = (jobId) => {
    const newExpanded = new Set(expandedJobs);
    if (newExpanded.has(jobId)) {
      newExpanded.delete(jobId);
    } else {
      newExpanded.add(jobId);
    }
    setExpandedJobs(newExpanded);
  };

  const handleApply = (jobId) => {
    if (!user) {
      toast.error('Please log in to apply for jobs');
      return;
    }
    
    if (user.role !== 'APPLICANT') {
      toast.error('Only applicants can apply for jobs');
      return;
    }

    applyMutation.mutate(jobId);
  };

  const handleDeleteJob = (job) => {
    if (window.confirm('Are you sure you want to delete this job? This action cannot be undone.')) {
      deleteMutation.mutate(job);
    }
  };

  const handleEditJob = (jobId) => {
    navigate(`/jobs/${jobId}/edit`);
  };

  // Filter and sort jobs
  const filteredAndSortedJobs = jobs
    .filter(job => {
      const matchesSearch = job.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          job.company.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          job.description.toLowerCase().includes(searchTerm.toLowerCase());
      
      const matchesRemote = filterRemote === 'all' || 
                           (filterRemote === 'remote' && job.remote) ||
                           (filterRemote === 'onsite' && !job.remote);
      
      return matchesSearch && matchesRemote;
    })
    .sort((a, b) => {
      switch (sortBy) {
        case 'newest':
          return new Date(b.createdAt || b.id) - new Date(a.createdAt || a.id);
        case 'oldest':
          return new Date(a.createdAt || a.id) - new Date(b.createdAt || b.id);
        case 'salary-high':
          return (b.salary || 0) - (a.salary || 0);
        case 'salary-low':
          return (a.salary || 0) - (b.salary || 0);
        default:
          return 0;
      }
    });

  if (isLoading) {
    return (
      <div className="container mt-4">
        <div className="row">
          <div className="col-12 text-center">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3">Loading jobs...</p>
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
              Error loading jobs: {error.message}
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
                <i className="bi bi-briefcase me-3 text-primary"></i>
                {user?.role === 'EMPLOYER' ? 'My Job Postings' : 'Available Jobs'}
              </h1>
              <p className="text-muted mb-0">
                {filteredAndSortedJobs.length} job{filteredAndSortedJobs.length !== 1 ? 's' : ''} found
              </p>
            </div>
            {user?.role === 'EMPLOYER' && (
              <button 
                className="btn btn-primary"
                onClick={() => navigate('/jobs/create')}
              >
                <i className="bi bi-plus-circle me-2"></i>
                Post New Job
              </button>
            )}
          </div>

          {/* Filters and Search */}
          <div className="card border-0 shadow-sm mb-4">
            <div className="card-body">
              <div className="row g-3">
                <div className="col-md-4">
                  <label className="form-label fw-semibold">
                    <i className="bi bi-search me-2"></i>
                    Search Jobs
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Search by title, company, or description..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
                <div className="col-md-3">
                  <label className="form-label fw-semibold">
                    <i className="bi bi-geo-alt me-2"></i>
                    Location Type
                  </label>
                  <select
                    className="form-select"
                    value={filterRemote}
                    onChange={(e) => setFilterRemote(e.target.value)}
                  >
                    <option value="all">All Locations</option>
                    <option value="remote">Remote Only</option>
                    <option value="onsite">On-site Only</option>
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label fw-semibold">
                    <i className="bi bi-sort-down me-2"></i>
                    Sort By
                  </label>
                  <select
                    className="form-select"
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                  >
                    <option value="newest">Newest First</option>
                    <option value="oldest">Oldest First</option>
                    <option value="salary-high">Highest Salary</option>
                    <option value="salary-low">Lowest Salary</option>
                  </select>
                </div>
                <div className="col-md-2 d-flex align-items-end">
                  <button 
                    className="btn btn-outline-secondary w-100"
                    onClick={() => {
                      setSearchTerm('');
                      setFilterRemote('all');
                      setSortBy('newest');
                    }}
                  >
                    <i className="bi bi-arrow-clockwise me-2"></i>
                    Reset
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Jobs List */}
          {filteredAndSortedJobs.length === 0 ? (
            <div className="text-center py-5">
              <i className="bi bi-briefcase text-muted" style={{ fontSize: '4rem' }}></i>
              <h3 className="mt-3 text-muted">No jobs found</h3>
              <p className="text-muted">
                {searchTerm || filterRemote !== 'all' 
                  ? 'Try adjusting your search criteria'
                  : 'No jobs are currently available'
                }
              </p>
              {user?.role === 'EMPLOYER' && (
                <button 
                  className="btn btn-primary"
                  onClick={() => navigate('/jobs/create')}
                >
                  <i className="bi bi-plus-circle me-2"></i>
                  Post Your First Job
                </button>
              )}
            </div>
          ) : (
            <div className="row g-4">
              {filteredAndSortedJobs.map((job) => (
                <div key={job.id} className="col-12">
                  <div className="card border-0 shadow-sm h-100">
                    <div className="card-body">
                      <div className="row align-items-start">
                        <div className="col-md-8">
                          <div className="d-flex justify-content-between align-items-start mb-2">
                            <h5 className="card-title mb-0 fw-bold text-primary">
                              {job.title}
                            </h5>
                            <div className="d-flex gap-2">
                              <span className={`badge ${job.remote ? 'bg-success' : 'bg-secondary'}`}>
                                <i className={`bi ${job.remote ? 'bi-wifi' : 'bi-geo-alt'} me-1`}></i>
                                {job.remote ? 'Remote' : 'On-site'}
                              </span>
                              <span className="badge bg-primary">
                                ${job.salary?.toLocaleString()}/year
                              </span>
                            </div>
                          </div>
                          
                          <h6 className="card-subtitle mb-3 text-muted">
                            <i className="bi bi-building me-2"></i>
                            {job.company}
                          </h6>
                          
                          <div className={`job-description ${expandedJobs.has(job.id) ? 'expanded' : 'collapsed'}`}>
                            <p className="card-text text-muted">
                              {job.description}
                            </p>
                          </div>
                          
                          <button
                            className="btn btn-link p-0 text-decoration-none"
                            onClick={() => toggleJobExpansion(job.id)}
                          >
                            {expandedJobs.has(job.id) ? 'Show less' : 'Show more'}
                          </button>
                        </div>

                        <div className="col-md-4 text-end">
                          <div className="d-flex flex-column gap-2">
                            {user?.role === 'APPLICANT' && (
                              <button
                                className="btn btn-success"
                                onClick={() => handleApply(job.id)}
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
                            
                            {user?.role === 'EMPLOYER' && (
                              <>
                                <button
                                  className="btn btn-outline-primary"
                                  onClick={() => handleEditJob(job.id)}
                                  title="Edit Job"
                                >
                                  <i className="bi bi-pencil me-2"></i>
                                  Edit
                                </button>
                                <button
                                  className="btn btn-outline-danger"
                                  onClick={() => handleDeleteJob(job)}
                                  disabled={deleteMutation.isPending}
                                  title="Delete Job"
                                >
                                  {deleteMutation.isPending ? (
                                    <>
                                      <span className="spinner-border spinner-border-sm me-2"></span>
                                      Deleting...
                                    </>
                                  ) : (
                                    <>
                                      <i className="bi bi-trash me-2"></i>
                                      Delete
                                    </>
                                  )}
                                </button>
                              </>
                            )}
                            
                            <button
                              className="btn btn-outline-secondary"
                              onClick={() => navigate(`/jobs/${job.id}`)}
                            >
                              <i className="bi bi-eye me-2"></i>
                              View Details
                            </button>
                          </div>
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

export default JobList; 