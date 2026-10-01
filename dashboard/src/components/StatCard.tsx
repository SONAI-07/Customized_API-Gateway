interface Props {
    title: string;
    value: string | number;
    accent?: string;
    subtitle?: string;
}

export default function StatCard({ title, value, accent = '#4fc3f7', subtitle }: Props) {
    return (
        <div className="card stat-card" style={{ borderTopColor: accent }}>
            <div className="stat-title">{title}</div>
            <div className="stat-value" style={{ color: accent }}>{value}</div>
            {subtitle && <div className="stat-subtitle">{subtitle}</div>}
        </div>
    );
}