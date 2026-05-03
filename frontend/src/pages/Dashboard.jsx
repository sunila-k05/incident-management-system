import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getIncidents } from '../api/api';
import PriorityBadge from '../components/PriorityBadge';
import StatusBadge from '../components/StatusBadge';
import { formatDistanceToNow } from 'date-fns';

const Dashboard = () => {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const fetchIncidents = async () => {
    try {
      const res = await getIncidents();
      setIncidents(res.data);
      setError(null);
    } catch (err) {
      setError('Cannot connect to backend. Is it running?');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIncidents();
    // Poll every 5 seconds for live updates
    const interval = setInterval(fetchIncidents, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="min-h-screen bg-gray-950 p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">
            Incident Management System
          </h1>
          <p className="text-gray-400 text-sm mt-1">
            Live incident feed — auto-refreshes every 5s
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse"></div>
          <span className="text-green-400 text-sm">Live</span>
        </div>
      </div>

      {/* Stats bar */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {['P0', 'P1', 'P2'].map(p => (
          <div key={p} className="bg-gray-900 rounded-lg p-4 border border-gray-800">
            <p className="text-gray-400 text-xs mb-1">{p} Incidents</p>
            <p className="text-2xl font-bold text-white">
              {incidents.filter(i => i.priority === p).length}
            </p>
          </div>
        ))}
        <div className="bg-gray-900 rounded-lg p-4 border border-gray-800">
          <p className="text-gray-400 text-xs mb-1">Total Active</p>
          <p className="text-2xl font-bold text-white">{incidents.length}</p>
        </div>
      </div>

      {/* Incident table */}
      {loading ? (
        <div className="text-center text-gray-400 py-20">Loading incidents...</div>
      ) : error ? (
        <div className="text-center text-red-400 py-20">{error}</div>
      ) : incidents.length === 0 ? (
        <div className="text-center text-gray-400 py-20">
          No active incidents. System is healthy.
        </div>
      ) : (
        <div className="bg-gray-900 rounded-lg border border-gray-800 overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-800">
                <th className="text-left p-4 text-gray-400 text-xs font-medium">PRIORITY</th>
                <th className="text-left p-4 text-gray-400 text-xs font-medium">TITLE</th>
                <th className="text-left p-4 text-gray-400 text-xs font-medium">COMPONENT</th>
                <th className="text-left p-4 text-gray-400 text-xs font-medium">STATUS</th>
                <th className="text-left p-4 text-gray-400 text-xs font-medium">SIGNALS</th>
                <th className="text-left p-4 text-gray-400 text-xs font-medium">OPENED</th>
              </tr>
            </thead>
            <tbody>
              {incidents.map(incident => (
                <tr
                  key={incident.id}
                  onClick={() => navigate(`/incidents/${incident.id}`)}
                  className="border-b border-gray-800 hover:bg-gray-800 cursor-pointer transition-colors"
                >
                  <td className="p-4">
                    <PriorityBadge priority={incident.priority} />
                  </td>
                  <td className="p-4 text-white text-sm max-w-xs truncate">
                    {incident.title}
                  </td>
                  <td className="p-4 text-gray-300 text-sm font-mono">
                    {incident.componentId}
                  </td>
                  <td className="p-4">
                    <StatusBadge state={incident.state} />
                  </td>
                  <td className="p-4 text-gray-300 text-sm">
                    {incident.signalCount}
                  </td>
                  <td className="p-4 text-gray-400 text-sm">
                    {formatDistanceToNow(new Date(incident.createdAt), { addSuffix: true })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
