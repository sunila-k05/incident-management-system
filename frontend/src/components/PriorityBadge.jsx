const PriorityBadge = ({ priority }) => {
  const styles = {
    P0: 'bg-red-500/20 text-red-400 border border-red-500/30',
    P1: 'bg-orange-500/20 text-orange-400 border border-orange-500/30',
    P2: 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30',
  };

  return (
    <span className={`px-2 py-1 rounded text-xs font-bold ${styles[priority]}`}>
      {priority}
    </span>
  );
};

export default PriorityBadge;
