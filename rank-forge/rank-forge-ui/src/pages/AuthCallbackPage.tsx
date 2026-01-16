import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../services/api';
import './AuthCallbackPage.css';

export const AuthCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const token = searchParams.get('token');
        
        if (!token) {
          setError('No authentication token received');
          setLoading(false);
          return;
        }

        // Save token to localStorage
        localStorage.setItem('authToken', token);

        // Fetch user data
        try {
          const currentUser = await authApi.getMe();
          localStorage.setItem('authUser', JSON.stringify(currentUser.user));
          
          // Trigger auth state update
          window.dispatchEvent(new Event('auth:login'));
          
          // Redirect to My Profile page
          navigate('/my-profile', { replace: true });
        } catch (error) {
          console.error('Failed to fetch user data:', error);
          setError('Failed to load user profile');
          setLoading(false);
        }
      } catch (error) {
        console.error('Authentication callback error:', error);
        setError('Authentication failed');
        setLoading(false);
      }
    };

    handleCallback();
  }, [searchParams, navigate]);

  if (loading) {
    return (
      <div className="auth-callback-page">
        <div className="auth-callback-container">
          <div className="loading-spinner"></div>
          <h2>Completing login...</h2>
          <p>Please wait while we set up your account.</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="auth-callback-page">
        <div className="auth-callback-container">
          <div className="error-icon">⚠️</div>
          <h2>Login Failed</h2>
          <p>{error}</p>
          <button onClick={() => navigate('/')} className="btn-primary">
            Go to Home
          </button>
        </div>
      </div>
    );
  }

  return null;
};
