const StatusBadge = ({ state }) => {
  const styles = {
    OPEN: 'bg-red-500/20 text-red-400 border border-red-500/30',
    INVESTIGATING: 'bg-blue-500/20 text-blue-400 border border-blue-500/30',
    RESOLVED: 'bg-green-500/20 text-green-400 border border-green-500/30',
    CLOSED: 'bg-gray-500/20 text-gray-400 border border-gray-500/30',
  };

  return (
    <span className={`px-2 py-1 rounded text-xs font-bold ${styles[state]}`}>
      {state}
    </span>
  );
};

export default StatusBadge;
