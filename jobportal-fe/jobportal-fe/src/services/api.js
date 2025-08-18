import axios from 'axios';

// Remove hardcoded base URL - proxy will handle this
// const API_BASE_URL = 'http://localhost:8080';

// Create axios instance with relative base URL
const api = axios.create({
  baseURL: '',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Important for CSRF tokens
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Authentication APIs
export const authAPI = {
  // Get CSRF token
  getCsrfToken: async () => {
    try {
      const response = await api.get('/login');
      const html = response.data;
      const csrfMatch = html.match(/name="_csrf" type="hidden" value="([^"]+)"/);
      const token = csrfMatch ? csrfMatch[1] : null;
      return token;
    } catch (error) {
      return null;
    }
  },

  // Form-based registration
  register: async (userData) => {
    const csrfToken = await authAPI.getCsrfToken();
    
    return api.post('/auth/api/register', {
      name: userData.name,
      email: userData.email,
      password: userData.password,
      role: userData.role,
      provider: 'LOCAL'
    }, {
      headers: {
        'X-XSRF-TOKEN': csrfToken,
        'Content-Type': 'application/json',
      },
    });
  },
  
  // Form-based login
  login: async (credentials) => {
    const csrfToken = await authAPI.getCsrfToken();
    
    const formData = new URLSearchParams();
    formData.append('email', credentials.email);
    formData.append('password', credentials.password);
    formData.append('_csrf', csrfToken);
    
    return api.post('/login', formData, {
      headers: {
        'X-XSRF-TOKEN': csrfToken,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
  },

  
  // Get available roles (for Google OAuth users)
  getRoles: () => api.get('/api/auth/roles'),
  
  // Get current user role
  getUserRole: () => api.get('/api/auth/user-role'),
  
  // Select role (for Google OAuth users)
  selectRole: (role) => api.post('/api/auth/select-role', { role }),
  
  // Google OAuth login (redirect to backend)
  googleLogin: () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  },
  
  // Test authentication by accessing protected endpoint
  getAllJobs: () => api.get('/api/v1/jobs'),
  

};

// Job APIs
export const jobAPI = {
  getAllJobs: () => api.get('/api/v1/jobs'),
  getJob: (id) => api.get(`/api/v1/jobs/${id}`),
  createJob: (jobData) => api.post('/api/v1/jobs', jobData),
  updateJob: (id, jobData) => api.put(`/api/v1/jobs/${id}`, jobData),
  deleteJob: (id, jobData) => api.delete(`/api/v1/jobs/${id}`, { data: jobData }),
};

// Application APIs
export const applicationAPI = {
  applyForJob: (jobId) => api.post(`/api/v1/application/${jobId}`),
  getMyApplications: () => api.get('/api/v1/application/my'),
  getRecruiterApplications: () => api.get('/api/v1/application/recruiter/all'),
  getJobApplications: (jobId) => api.get(`/api/v1/application/recruiter/job/${jobId}`),
  // Note: These endpoints might not exist in the current API, but we'll include them for completeness
  getApplication: (id) => api.get(`/api/v1/application/${id}`),
  updateApplication: (id, data) => api.put(`/api/v1/application/${id}`, data),
  deleteApplication: (id) => api.delete(`/api/v1/application/${id}`),
};

export default api; 