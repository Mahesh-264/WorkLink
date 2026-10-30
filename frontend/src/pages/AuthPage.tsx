import { type FormEvent, useState } from 'react';
import { ArrowLeft, Briefcase, LogIn, Search, UserPlus, Users } from 'lucide-react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import api from '../lib/api';
import { clearSession } from '../lib/auth';

type AuthRole = 'client' | 'freelancer';
type AuthMode = 'login' | 'register';

interface AuthUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: 'CLIENT' | 'FREELANCER';
}

interface LoginResponse extends AuthUser {
  accessToken: string;
  tokenType: 'Bearer';
}

interface ApiError {
  message?: string;
  fieldErrors?: Record<string, string>;
  error?: string;
}

const roleContent: Record<AuthRole, { apiRole: AuthUser['role']; label: string; description: string; Icon: typeof Users }> = {
  client: {
    apiRole: 'CLIENT',
    label: 'Client',
    description: 'Post projects and connect with the right specialist.',
    Icon: Users,
  },
  freelancer: {
    apiRole: 'FREELANCER',
    label: 'Freelancer',
    description: 'Build your profile and discover work aligned with your expertise.',
    Icon: Search,
  },
};

const isAuthRole = (role: string | undefined): role is AuthRole => role === 'client' || role === 'freelancer';
const isAuthMode = (mode: string | undefined): mode is AuthMode => mode === 'login' || mode === 'register';

export const AuthPage = () => {
  const { role, mode } = useParams();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!isAuthRole(role) || !isAuthMode(mode)) {
    return <Navigate to="/" replace />;
  }

  const content = roleContent[role];
  const isRegistration = mode === 'register';
  const alternateMode: AuthMode = isRegistration ? 'login' : 'register';
  const Icon = content.Icon;

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setMessage(null);
    clearSession();

    const formData = new FormData(event.currentTarget);
    const email = String(formData.get('email') ?? '');
    const password = String(formData.get('password') ?? '');

    try {
      if (isRegistration) {
        await api.post<AuthUser>('/auth/register', {
          firstName: String(formData.get('firstName') ?? ''),
          lastName: String(formData.get('lastName') ?? ''),
          email,
          password,
          role: content.apiRole,
        });
        setMessage('Your account has been created. You can now sign in.');
      } else {
        const { data } = await api.post<LoginResponse>('/auth/login', { email, password });
        if (data.role !== content.apiRole) {
          setError(`This account belongs to the ${data.role === 'CLIENT' ? 'Client' : 'Freelancer'} portal.`);
          return;
        }
        localStorage.setItem('worklink_token', data.accessToken);
        localStorage.setItem('worklink_user', JSON.stringify({
          id: data.id,
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email,
          role: data.role,
        }));
        navigate(data.role === 'CLIENT' ? '/client/dashboard' : '/freelancer/dashboard', { replace: true });
      }
    } catch (requestError) {
      if (axios.isAxiosError<ApiError>(requestError)) {
        const data = requestError.response?.data;
        if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
          setError(Object.values(data.fieldErrors).join('. '));
        } else if (data?.message) {
          setError(data.message);
        } else if (data?.error) {
          setError(data.error);
        } else if (typeof data === 'string' && (data as string).trim().length > 0) {
          setError(data);
        } else if (requestError.response?.status === 409) {
          setError('An account with this email already exists.');
        } else if (requestError.response?.status === 401) {
          setError(isRegistration ? 'Authentication failed. Please verify your details.' : 'Invalid email or password.');
        } else if (requestError.code === 'ERR_NETWORK') {
          setError('Unable to connect to the backend server. Please verify your connection.');
        } else {
          setError(requestError.message || 'We could not complete your request. Please try again.');
        }
      } else if (requestError instanceof Error) {
        setError(requestError.message);
      } else {
        setError('We could not complete your request. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8 sm:px-6 sm:py-12">
      <div className="mx-auto w-full max-w-md">
        <Link to="/" className="mb-8 inline-flex items-center gap-2 text-sm font-medium text-slate-600 transition-colors hover:text-indigo-600">
          <ArrowLeft className="h-4 w-4" />
          Back to WorkLink
        </Link>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="mb-6 flex items-start gap-4">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-indigo-600 text-white">
              <Briefcase className="h-5 w-5" />
            </div>
            <div>
              <div className="mb-1 flex items-center gap-2 text-sm font-semibold text-indigo-700">
                <Icon className="h-4 w-4" />
                {content.label} portal
              </div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                {isRegistration ? `Create a ${content.label.toLowerCase()} account` : `Sign in as a ${content.label.toLowerCase()}`}
              </h1>
              <p className="mt-2 text-sm leading-6 text-slate-500">{content.description}</p>
            </div>
          </div>

          <div className="mb-6 grid grid-cols-2 rounded-xl bg-slate-100 p-1 text-sm font-medium">
            <Link
              to={`/auth/${role}/login`}
              className={`rounded-lg px-3 py-2 text-center transition-colors ${!isRegistration ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'}`}
            >
              Sign in
            </Link>
            <Link
              to={`/auth/${role}/register`}
              className={`rounded-lg px-3 py-2 text-center transition-colors ${isRegistration ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-900'}`}
            >
              Register
            </Link>
          </div>

          <form className="space-y-4" onSubmit={submit}>
            {isRegistration && (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <label className="block text-sm font-medium text-slate-700">
                  First name
                  <input name="firstName" required maxLength={100} autoComplete="given-name" className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100" />
                </label>
                <label className="block text-sm font-medium text-slate-700">
                  Last name
                  <input name="lastName" required maxLength={100} autoComplete="family-name" className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100" />
                </label>
              </div>
            )}

            <label className="block text-sm font-medium text-slate-700">
              Email address
              <input name="email" type="email" required maxLength={150} autoComplete="email" className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100" />
            </label>
            <label className="block text-sm font-medium text-slate-700">
              Password
              <input name="password" type="password" required minLength={8} maxLength={72} autoComplete={isRegistration ? 'new-password' : 'current-password'} className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100" />
            </label>

            {error && <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
            {message && <p role="status" className="rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</p>}

            <button type="submit" disabled={submitting} className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 font-medium text-white transition-colors hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-indigo-400">
              {isRegistration ? <UserPlus className="h-4 w-4" /> : <LogIn className="h-4 w-4" />}
              {submitting ? 'Please wait…' : isRegistration ? 'Create account' : 'Sign in'}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            {isRegistration ? 'Already have an account?' : 'New to WorkLink?'}{' '}
            <Link to={`/auth/${role}/${alternateMode}`} className="font-semibold text-indigo-600 hover:text-indigo-700">
              {isRegistration ? 'Sign in' : 'Create an account'}
            </Link>
          </p>
        </section>
      </div>
    </main>
  );
};

export default AuthPage;
