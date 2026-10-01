import { ServiceMetrics } from '../types';

export default function ServiceTable({ services }: { services: ServiceMetrics[] }) {
    if (services.length === 0) {
        return (
            <div className="card">
                <h3>Service Registry & Load Distribution</h3>
                <p className="muted">No services registered yet.</p>
            </div>
        );
    }

    return (
        <div className="card">
            <h3>Service Registry & Load Distribution</h3>
            <table className="svc-table">
                <thead>
                <tr>
                    <th>Service</th>
                    <th>Instances</th>
                    <th>Instance ID</th>
                    <th>Host:Port</th>
                    <th>Active Conns</th>
                </tr>
                </thead>
                <tbody>
                {services.map(svc =>
                    svc.instances.map((inst, idx) => (
                        <tr key={inst.instanceID} className={inst.instanceID === svc.busiestInstance ? 'busiest' : ''}>
                            {idx === 0 && (
                                <>
                                    <td rowSpan={svc.instances.length} className="svc-name">{svc.serviceName}</td>
                                    <td rowSpan={svc.instances.length}>{svc.instanceCount}</td>
                                </>
                            )}
                            <td>
                                {inst.instanceID}
                                {inst.instanceID === svc.busiestInstance && <span className="badge">BUSIEST</span>}
                            </td>
                            <td>{inst.host}:{inst.port}</td>
                            <td>{inst.activeConnections}</td>
                        </tr>
                    ))
                )}
                </tbody>
            </table>
        </div>
    );
}