import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Briefcase, 
  Search, 
  ShieldCheck, 
  Users, 
  Layers, 
  ArrowRight, 
  Activity,
  Code2
} from 'lucide-react';
import api from '../lib/api';

interface HealthStatus {
  status: string;
  application: string;
}

export const LandingPage: React.FC = () => {
  const [backendHealth, setBackendHealth] = useState<HealthStatus | null>(null);
  const [loadingHealth, setLoadingHealth] = useState<boolean>(true);

  useEffect(() => {
    // Check backend connection
    api.get<HealthStatus>('/health')
      .then((res) => {
        setBackendHealth(res.data);
      })
      .catch(() => {
        // Backend not currently reachable or in standalone frontend development
        setBackendHealth(null);
      })
      .finally(() => {
        setLoadingHealth(false);
      });
  }, []);

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900 selection:bg-indigo-500 selection:text-white">
      {/* Navigation Header */}
      <header className="sticky top-0 z-50 bg-white/90 backdrop-blur border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-sm shadow-indigo-200">
              <Briefcase className="h-5 w-5" />
            </div>
            <span className="text-xl font-bold tracking-tight text-slate-900">WorkLink</span>
          </div>

          {/* System Status Pill */}
          <div className="flex items-center gap-2 text-xs font-medium px-3 py-1.5 rounded-full bg-slate-100 border border-slate-200">
            <Activity className="h-3.5 w-3.5 text-slate-500" />
            <span className="text-slate-600">Backend API:</span>
            {loadingHealth ? (
              <span className="text-amber-600 font-semibold animate-pulse">Checking...</span>
            ) : backendHealth && backendHealth.status === 'UP' ? (
              <span className="inline-flex items-center gap-1 text-emerald-600 font-semibold">
                <span className="h-2 w-2 rounded-full bg-emerald-500"></span>
                Connected ({backendHealth.application})
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 text-slate-500">
                <span className="h-2 w-2 rounded-full bg-slate-400"></span>
                Ready (Port 8080)
              </span>
            )}
          </div>
        </div>
      </header>

      {/* Main Hero Section */}
      <main className="flex-1">
        <section className="relative overflow-hidden pt-16 pb-20 lg:pt-24 lg:pb-28">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-3xl mx-auto">
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-900 leading-tight mb-6">
                Intelligent Matching for{' '}
                <span className="bg-gradient-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
                  Projects & Specialized Talent
                </span>
              </h1>

              <p className="text-lg sm:text-xl text-slate-600 leading-relaxed mb-10">
                WorkLink introduces a dynamic, context-aware framework that evaluates project requirements,
                freelancer capability profiles, and multi-dimensional performance metrics to deliver high-precision recommendations.
              </p>

              {/* Dual Entry Points */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-xl mx-auto mb-14">
                <div className="p-6 bg-white rounded-2xl border border-slate-200/90 shadow-sm hover:shadow-md transition-shadow text-left flex flex-col justify-between">
                  <div>
                    <div className="h-10 w-10 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center mb-4">
                      <Users className="h-5 w-5" />
                    </div>
                    <h3 className="text-lg font-semibold text-slate-900 mb-1">Find the Right Talent</h3>
                    <p className="text-sm text-slate-500 mb-4">
                      Define project requirements, budget, and deadlines to receive compatibility-ranked freelancer recommendations.
                    </p>
                  </div>
                  <Link
                    to="/auth/client/login"
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 text-white font-medium hover:bg-indigo-700 transition-colors cursor-pointer text-sm shadow-sm"
                  >
                    <span>Client Entry Point</span>
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>

                <div className="p-6 bg-white rounded-2xl border border-slate-200/90 shadow-sm hover:shadow-md transition-shadow text-left flex flex-col justify-between">
                  <div>
                    <div className="h-10 w-10 rounded-lg bg-violet-100 text-violet-600 flex items-center justify-center mb-4">
                      <Search className="h-5 w-5" />
                    </div>
                    <h3 className="text-lg font-semibold text-slate-900 mb-1">Find the Right Project</h3>
                    <p className="text-sm text-slate-500 mb-4">
                      Build your capability profile, showcase verified competencies, and discover projects aligned with your expertise.
                    </p>
                  </div>
                  <Link
                    to="/auth/freelancer/login"
                    className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-white text-slate-800 font-medium border border-slate-300 hover:bg-slate-50 transition-colors cursor-pointer text-sm shadow-sm"
                  >
                    <span>Freelancer Entry Point</span>
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              </div>
            </div>

            {/* Core Architectural Pillars */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
              <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-sm">
                <div className="h-9 w-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center mb-3">
                  <Layers className="h-4 w-4" />
                </div>
                <h4 className="text-base font-semibold text-slate-900 mb-2">Project Requirement Profile</h4>
                <p className="text-sm text-slate-500 leading-relaxed">
                  Synthesizes required skills, complexity tier, deadline velocity, budget range, and specific deliverable goals.
                </p>
              </div>

              <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-sm">
                <div className="h-9 w-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center mb-3">
                  <ShieldCheck className="h-4 w-4" />
                </div>
                <h4 className="text-base font-semibold text-slate-900 mb-2">Freelancer Capability Profile</h4>
                <p className="text-sm text-slate-500 leading-relaxed">
                  Models multi-skill proficiency, portfolio relevance, cumulative experience, and dynamic trust parameters.
                </p>
              </div>

              <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-sm">
                <div className="h-9 w-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center mb-3">
                  <Code2 className="h-4 w-4" />
                </div>
                <h4 className="text-base font-semibold text-slate-900 mb-2">Context-Aware Matching</h4>
                <p className="text-sm text-slate-500 leading-relaxed">
                  Generates deterministic, explainable compatibility scores with component breakdowns for project owners.
                </p>
              </div>
            </div>

            {/* Technology Stack Verification Bar */}
            <div className="mt-16 pt-8 border-t border-slate-200/70 max-w-4xl mx-auto">
              <div className="text-center mb-4">
                <p className="text-xs uppercase tracking-wider font-semibold text-slate-400">
                  Target Architecture & Engineered Stack
                </p>
              </div>
              <div className="flex flex-wrap justify-center items-center gap-3 text-xs font-medium text-slate-600">
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">Java 21 LTS</span>
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">Spring Boot 3</span>
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">Spring Security + JWT</span>
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">Spring Data JPA</span>
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">PostgreSQL 18</span>
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">React 19 + TypeScript</span>
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">Tailwind CSS</span>
                <span className="px-3 py-1 bg-white border border-slate-200 rounded-full">React Router v7</span>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 py-8 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <p className="font-medium text-slate-700 mb-1">
            WorkLink: A Dynamic Context-Aware Framework for Intelligent Freelancer–Project Matching
          </p>
          <p className="text-slate-400">
            Connecting clients with specialized talent.
          </p>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;
