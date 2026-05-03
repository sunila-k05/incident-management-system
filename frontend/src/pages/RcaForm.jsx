import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { submitRca, getRca } from '../api/api';
import { format } from 'date-fns';

const ROOT_CAUSE_CATEGORIES = [
  'INFRASTRUCTURE_FAILURE',
  'DEPLOYMENT_REGRESSION',
  'TRAFFIC_SPIKE',
  'DEPENDENCY_FAILURE',
  'CONFIGURATION_ERROR',
  'HARDWARE_FAILURE',
  'NETWORK_ISSUE',
  'UNKNOWN'
];

const RcaForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const now = format(new Date(), "yyyy-MM-dd'T'HH:mm");

  const [form, setForm] = useState({
    rootCauseCategory: '',
    fixApplied: '',
    preventionSteps: '',
    incidentStart: now,
    incidentEnd: now,
    submittedBy: ''
  });

  // Load existing RCA if any
  useEffect(() => {
    getRca(id).then(res => {
      const rca = res.data;
      setForm({
        rootCauseCategory: rca.rootCauseCategory || '',
        fixApplied: rca.fixApplied || '',
        preventionSteps: rca.preventionSteps || '',
        incidentStart: rca.incidentStart
          ? format(new Date(rca.incidentStart), "yyyy-MM-dd'T'HH:mm")
          : now,
        incidentEnd: rca.incidentEnd
          ? format(new Date(rca.incidentEnd), "yyyy-MM-dd'T'HH:mm")
          : now,
        submittedBy: rca.submittedBy || ''
      });
    }).catch(() => {});
  }, [id]);

  const handleSubmit = async () => {
    if (!form.rootCauseCategory || !form.fixApplied || !form.preventionSteps) {
      setError('All fields are required.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await submitRca(id, {
        ...form,
        incidentStart: new Date(form.incidentStart).toISOString(),
        incidentEnd: new Date(form.incidentEnd).toISOString()
      });
      setSuccess(true);
      setTimeout(() => navigate(`/incidents/${id}`), 1500);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to submit RCA');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-950 p-6">
      <button
        onClick={() => navigate(`/incidents/${id}`)}
        className="text-gray-400 hover:text-white text-sm mb-6 flex items-center gap-2"
      >
        ← Back to Incident
      </button>

      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-white mb-2">
          Root Cause Analysis
        </h1>
        <p className="text-gray-400 text-sm mb-8">
          Complete all fields to close the incident.
        </p>

        <div className="bg-gray-900 rounded-lg border border-gray-800 p-6 space-y-6">

          {/* Timeline */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-gray-400 text-xs block mb-2">
                Incident Start
              </label>
              <input
                type="datetime-local"
                value={form.incidentStart}
                onChange={e => setForm({...form, incidentStart: e.target.value})}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-white text-sm"
              />
            </div>
            <div>
              <label className="text-gray-400 text-xs block mb-2">
                Incident End
              </label>
              <input
                type="datetime-local"
                value={form.incidentEnd}
                onChange={e => setForm({...form, incidentEnd: e.target.value})}
                className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-white text-sm"
              />
            </div>
          </div>

          {/* Root cause category */}
          <div>
            <label className="text-gray-400 text-xs block mb-2">
              Root Cause Category
            </label>
            <select
              value={form.rootCauseCategory}
              onChange={e => setForm({...form, rootCauseCategory: e.target.value})}
              className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-white text-sm"
            >
              <option value="">Select a category...</option>
              {ROOT_CAUSE_CATEGORIES.map(cat => (
                <option key={cat} value={cat}>{cat.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </div>

          {/* Fix applied */}
          <div>
            <label className="text-gray-400 text-xs block mb-2">
              Fix Applied
            </label>
            <textarea
              value={form.fixApplied}
              onChange={e => setForm({...form, fixApplied: e.target.value})}
              rows={4}
              placeholder="Describe exactly what was done to resolve the incident..."
              className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-white text-sm resize-none"
            />
          </div>

          {/* Prevention steps */}
          <div>
            <label className="text-gray-400 text-xs block mb-2">
              Prevention Steps
            </label>
            <textarea
              value={form.preventionSteps}
              onChange={e => setForm({...form, preventionSteps: e.target.value})}
              rows={4}
              placeholder="What steps will prevent this from happening again..."
              className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-white text-sm resize-none"
            />
          </div>

          {/* Submitted by */}
          <div>
            <label className="text-gray-400 text-xs block mb-2">
              Submitted By
            </label>
            <input
              type="text"
              value={form.submittedBy}
              onChange={e => setForm({...form, submittedBy: e.target.value})}
              placeholder="engineer@company.com"
              className="w-full bg-gray-800 border border-gray-700 rounded px-3 py-2 text-white text-sm"
            />
          </div>

          {error && (
            <div className="p-3 bg-red-500/10 border border-red-500/30 rounded text-red-400 text-sm">
              {error}
            </div>
          )}

          {success && (
            <div className="p-3 bg-green-500/10 border border-green-500/30 rounded text-green-400 text-sm">
              RCA submitted successfully! Redirecting...
            </div>
          )}

          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="w-full bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white py-3 rounded font-medium text-sm"
          >
            {submitting ? 'Submitting...' : 'Submit RCA'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default RcaForm;
