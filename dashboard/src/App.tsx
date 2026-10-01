import { useGatewayMetrics } from './hooks/useGatewayMetrics';
import StatCard from './components/StatCard';
import TrafficChart from './components/TrafficChart';
import BlockedBreakdown from './components/BlockedBreakdown';
import ServiceTable from './components/ServiceTable';

export default function App() {
    const { summary, history, error, lastUpdated } = useGatewayMetrics();

    const totalBlocked = summary ? Object.values(summary.blockedRequests).reduce((a, b) => a + b, 0) : 0;
    const totalConns = summary
        ? summary.services.reduce((a, s) => a + s.instances.reduce((x, i) => x + i.activeConnections, 0), 0)
        : 0;
    const totalInstances = summary ? summary.services.reduce((a, s) => a + s.instanceCount, 0) : 0;

    return (
        <div className="app">
            <header className="header">
                <h1>⚡ Hyper-Reactive API Gateway</h1>
                <div className="header-meta">
                    {error
                        ? <span className="status error">● disconnected ({error})</span>
                        : <span className="status ok">● live</span>}
                    {lastUpdated && <span className="muted">updated {lastUpdated.toLocaleTimeString()}</span>}
                </div>
            </header>

            <section className="stat-grid">
                <StatCard title="Total Requests"   value={summary?.totalRequests ?? 0}    accent="#4fc3f7" />
                <StatCard title="Accepted"         value={summary?.acceptedRequests ?? 0} accent="#66bb6a" />
                <StatCard title="Blocked"          value={totalBlocked}                   accent="#ef5350" />
                <StatCard title="Active Connections" value={totalConns}                   accent="#ffa726" />
                <StatCard title="Live Instances"   value={totalInstances}                 accent="#ab47bc"
                          subtitle={`${summary?.services.length ?? 0} services`} />
            </section>

            <section className="mid-grid">
                <TrafficChart data={history} />
                <BlockedBreakdown blocked={summary?.blockedRequests ?? {}} />
            </section>

            <ServiceTable services={summary?.services ?? []} />
        </div>
    );
}