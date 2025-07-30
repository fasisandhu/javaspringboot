import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { jobAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const schema = yup.object({
  title: yup.string().required('Job title is required').min(3, 'Title must be at least 3 characters'),
  description: yup.string().required('Job description is required').min(10, 'Description must be at least 10 characters'),
  company: yup.string().required('Company name is required'),
  salary: yup.number().required('Salary is required').positive('Salary must be positive'),
  remote: yup.boolean(),
}).required();

const JobForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isEditing = !!id;

  const { data: jobResponse, isLoading: isLoadingJob } = useQuery({
    queryKey: ['job', id],
    queryFn: () => jobAPI.getJob(id),
    enabled: isEditing && !!id,
  });

  // Extract job data from response
  const job = jobResponse?.data || jobResponse;

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm({
    resolver: yupResolver(schema),
    defaultValues: {
      remote: false,
    },
  });

  const createMutation = useMutation({
    mutationFn: (data) => jobAPI.createJob(data),
    onSuccess: () => {
      toast.success('Job created successfully!');
      queryClient.invalidateQueries(['jobs']);
      navigate('/jobs');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to create job');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data) => jobAPI.updateJob(id, data),
    onSuccess: () => {
      toast.success('Job updated successfully!');
      queryClient.invalidateQueries(['jobs']);
      queryClient.invalidateQueries(['job', id]);
      navigate('/jobs');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to update job');
    },
  });

  useEffect(() => {
    if (job && isEditing) {
      reset({
        title: job.title,
        description: job.description,
        company: job.company,
        salary: job.salary,
        remote: job.remote,
      });
    }
  }, [job, isEditing, reset]);

  const onSubmit = async (data) => {
    if (user?.role !== 'EMPLOYER' && user?.role !== 'ADMIN') {
      toast.error('Only employers can create or edit jobs');
      return;
    }

    setIsSubmitting(true);
    try {
      if (isEditing) {
        await updateMutation.mutateAsync(data);
      } else {
        await createMutation.mutateAsync(data);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoadingJob) {
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

  return (
    <div className="container mt-4">
      <div className="row justify-content-center">
        <div className="col-lg-8">
          <div className="d-flex justify-content-between align-items-center mb-4">
            <h1>
              <i className="bi bi-plus-circle me-2"></i>
              {isEditing ? 'Edit Job' : 'Create New Job'}
            </h1>
            <button 
              className="btn btn-outline-secondary"
              onClick={() => navigate('/jobs')}
            >
              <i className="bi bi-arrow-left me-2"></i>
              Back to Jobs
            </button>
          </div>

          <div className="card">
            <div className="card-body">
              <form onSubmit={handleSubmit(onSubmit)}>
                <div className="row">
                  {/* Job Title */}
                  <div className="col-md-12 mb-3">
                    <label htmlFor="title" className="form-label">
                      Job Title *
                    </label>
                    <input
                      type="text"
                      className={`form-control ${errors.title ? 'is-invalid' : ''}`}
                      id="title"
                      {...register('title')}
                      placeholder="e.g., Senior Software Engineer"
                    />
                    {errors.title && (
                      <div className="invalid-feedback">{errors.title.message}</div>
                    )}
                  </div>

                  {/* Company */}
                  <div className="col-md-6 mb-3">
                    <label htmlFor="company" className="form-label">
                      Company *
                    </label>
                    <input
                      type="text"
                      className={`form-control ${errors.company ? 'is-invalid' : ''}`}
                      id="company"
                      {...register('company')}
                      placeholder="e.g., Tech Corp"
                    />
                    {errors.company && (
                      <div className="invalid-feedback">{errors.company.message}</div>
                    )}
                  </div>

                  {/* Salary */}
                  <div className="col-md-6 mb-3">
                    <label htmlFor="salary" className="form-label">
                      Annual Salary ($) *
                    </label>
                    <input
                      type="number"
                      className={`form-control ${errors.salary ? 'is-invalid' : ''}`}
                      id="salary"
                      {...register('salary')}
                      placeholder="e.g., 80000"
                    />
                    {errors.salary && (
                      <div className="invalid-feedback">{errors.salary.message}</div>
                    )}
                  </div>

                  {/* Remote Work */}
                  <div className="col-md-12 mb-3">
                    <div className="form-check">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id="remote"
                        {...register('remote')}
                      />
                      <label className="form-check-label" htmlFor="remote">
                        Remote Position
                      </label>
                    </div>
                  </div>

                  {/* Job Description */}
                  <div className="col-md-12 mb-3">
                    <label htmlFor="description" className="form-label">
                      Job Description *
                    </label>
                    <textarea
                      className={`form-control ${errors.description ? 'is-invalid' : ''}`}
                      id="description"
                      rows="6"
                      {...register('description')}
                      placeholder="Describe the role, responsibilities, and what you're looking for in a candidate..."
                    />
                    {errors.description && (
                      <div className="invalid-feedback">{errors.description.message}</div>
                    )}
                  </div>
                </div>

                <div className="d-flex justify-content-end gap-2 mt-4">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => navigate('/jobs')}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-2"></span>
                        {isEditing ? 'Updating...' : 'Creating...'}
                      </>
                    ) : (
                      <>
                        <i className="bi bi-check-circle me-2"></i>
                        {isEditing ? 'Update Job' : 'Create Job'}
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default JobForm; 