import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getIncident, getSignals,
  investigateIncident, resolveIncident, closeIncident
} from '../api/api';
import PriorityBadge from '../components/PriorityBadge';
import StatusBadge from '../components/StatusBadge';
import { formatDistanceToNow, format } from 'date-fns';

const IncidentDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [incident, setIncident] = useState(null);
  const [signals, setSignals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionError, setActionError] = useState(null);

  const fetchData = async () => {
    try {
      const [incRes, sigRes] = await Promise.all([
        getIncident(id),
        getSignals(id)
      ]);
      setIncident(incRes.data);
      setSignals(sigRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [id]);

  const handleTransition = async (action) => {
    try {
      setActionError(null);
      if (action === 'investigate') await investigateIncident(id);
      if (action === 'resolve') await resolveIncident(id);
      if (action === 'close') await closeIncident(id);
      fetchData();
    } catch (err) {
      setActionError(err.response?.data?.error || 'Action failed');
    }
  };

  if (loading) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <p className="text-gray-400">Loading incident...</p>
    </div>
  );

  if (!incident) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <p className="text-red-400">Incident not found</p>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-950 p-6">
      {/* Back button */}
      <button
        onClick={() => navigate('/')}
        className="text-gray-400 hover:text-white text-sm mb-6 flex items-center gap-2"
      >
        ← Back to Dashboard
      </button>

      {/* Incident header */}
      <div className="bg-gray-900 rounded-lg border border-gray-800 p-6 mb-6">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <PriorityBadge priority={incident.priority} />
              <StatusBadge state={incident.state} />
            </div>
            <h1 className="text-xl font-bold text-white mb-1">
              {incident.title}
            </h1>
            <p className="text-gray-400 text-sm font-mono">
              {incident.componentId} • {incident.componentType}
            </p>
          </div>

          {/* Action buttons */}
          <div className="flex gap-2">
            {incident.state === 'OPEN' && (
              <button
                onClick={() => handleTransition('investigate')}
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded text-sm"
              >
                Investigate
              </button>
            )}
            {incident.state === 'INVESTIGATING' && (
              <button
                onClick={() => handleTransition('resolve')}
                className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded text-sm"
              >
                Resolve
              </button>
            )}
            {incident.state === 'RESOLVED' && (
              <>
                <button
                  onClick={() => navigate(`/incidents/${id}/rca`)}
                  className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded text-sm"
                >
                  Submit RCA
                </button>
                <button
                  onClick={() => handleTransition('close')}
                  className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded text-sm"
                >
                  Close
                </button>
              </>
            )}
          </div>
        </div>

        {actionError && (
          <div className="mt-4 p-3 bg-red-500/10 border border-red-500/30 rounded text-red-400 text-sm">
            {actionError}
          </div>
        )}

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4 mt-6">
          <div>
            <p className="text-gray-400 text-xs">Signals</p>
            <p className="text-white font-bold text-lg">{incident.signalCount}</p>
          </div>
          <div>
            <p className="text-gray-400 text-xs">Started</p>
            <p className="text-white text-sm">
              {formatDistanceToNow(new Date(incident.createdAt), { addSuffix: true })}
            </p>
          </div>
          <div>
            <p className="text-gray-400 text-xs">MTTR</p>
            <p className="text-white text-sm">
              {incident.mttrMinutes ? `${incident.mttrMinutes} min` : 'In progress'}
            </p>
          </div>
          <div>
            <p className="text-gray-400 text-xs">ID</p>
            <p className="text-white text-xs font-mono truncate">{incident.id}</p>
          </div>
        </div>
      </div>

      {/* Raw signals */}
      <div className="bg-gray-900 rounded-lg border border-gray-800 p-6">
        <h2 className="text-white font-semibold mb-4">
          Raw Signals ({signals.length})
        </h2>
        {signals.length === 0 ? (
          <p className="text-gray-400 text-sm">No signals linked yet.</p>
        ) : (
          <div className="space-y-2 max-h-96 overflow-y-auto">
            {signals.map(signal => (
              <div
                key={signal.id}
                className="bg-gray-800 rounded p-3 text-sm font-mono"
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-blue-400">{signal.signalType}</span>
                  <span className="text-gray-400 text-xs">
                    {signal.timestamp ? format(new Date(signal.timestamp), 'HH:mm:ss') : ''}
                  </span>
                </div>
                <div className="text-gray-300">
                  Value: <span className="text-yellow-400">{signal.value} {signal.unit}</span>
                  {' '}/ Threshold: <span className="text-red-400">{signal.threshold} {signal.unit}</span>
                </div>
                <div className="text-gray-500 text-xs mt-1">
                  Region: {signal.region}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default IncidentDetail;
