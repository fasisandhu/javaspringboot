import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import ErrorBoundary from './components/ErrorBoundary';
import Header from './components/layout/Header';
import Dashboard from './pages/Dashboard';
import JobList from './pages/JobList';
import JobDetail from './pages/JobDetail';
import JobForm from './pages/JobForm';
import ApplicationList from './pages/ApplicationList';
import Login from './pages/Login';
import Register from './pages/Register';
import RoleSelection from './pages/RoleSelection';
import OAuthSuccess from './pages/OAuthSuccess';
import './App.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <Router>
            <div className="App">
              <Header />
              <main className="main-content">
                <Routes>
                  <Route path="/login" element={<Login />} />
                  <Route path="/register" element={<Register />} />
                  <Route path="/role-selection" element={<RoleSelection />} />
                  <Route path="/oauth-success" element={<OAuthSuccess />} />
                  <Route path="/auth/role-selection" element={<RoleSelection />} />
                  <Route path="/auth/success" element={<Dashboard />} />
                  <Route path="/" element={<Navigate to="/dashboard" replace />} />
                  <Route
                    path="/dashboard"
                    element={
                      <ProtectedRoute>
                        <Dashboard />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/jobs"
                    element={
                      <ProtectedRoute>
                        <JobList />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/jobs/:id"
                    element={
                      <ProtectedRoute>
                        <JobDetail />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/jobs/create"
                    element={
                      <ProtectedRoute>
                        <JobForm />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/jobs/:id/edit"
                    element={
                      <ProtectedRoute>
                        <JobForm />
                      </ProtectedRoute>
                    }
                  />
                  <Route
                    path="/applications"
                    element={
                      <ProtectedRoute>
                        <ApplicationList />
                      </ProtectedRoute>
                    }
                  />
                </Routes>
              </main>
              <Toaster
                position="top-right"
                toastOptions={{
                  duration: 4000,
                  style: {
                    background: '#363636',
                    color: '#fff',
                  },
                  success: {
                    duration: 3000,
                    iconTheme: {
                      primary: '#4ade80',
                      secondary: '#fff',
                    },
                  },
                  error: {
                    duration: 5000,
                    iconTheme: {
                      primary: '#ef4444',
                      secondary: '#fff',
                    },
                  },
                }}
              />
            </div>
          </Router>
        </AuthProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
