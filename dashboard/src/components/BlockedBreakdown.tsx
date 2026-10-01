const COLORS: Record<string, string> = {
    rate_limited: '#ffa726',
    unauthorized: '#ef5350',
    registry_forbidden: '#ab47bc',
};

export default function BlockedBreakdown({ blocked }: { blocked: Record<string, number> }) {
    const entries = Object.entries(blocked);
    const max = Math.max(1, ...entries.map(([, v]) => v));

    return (
        <div className="card">
            <h3>Blocked Requests by Reason</h3>
            {entries.length === 0 && <p className="muted">No blocked requests yet 🎉</p>}
            {entries.map(([reason, count]) => (
                <div key={reason} className="bar-row">
                    <span className="bar-label">{reason}</span>
                    <div className="bar-track">
                        <div
                            className="bar-fill"
                            style={{ width: `${(count / max) * 100}%`, background: COLORS[reason] ?? '#8b93a7' }}
                        />
                    </div>
                    <span className="bar-value">{count}</span>
                </div>
            ))}
        </div>
    );
}