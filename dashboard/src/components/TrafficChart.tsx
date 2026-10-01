import {
    CartesianGrid, Legend, Line, LineChart,
    ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { TrafficPoint } from '../types';

export default function TrafficChart({ data }: { data: TrafficPoint[] }) {
    return (
        <div className="card">
            <h3>Live Traffic (requests / sec)</h3>
            <ResponsiveContainer width="100%" height={260}>
                <LineChart data={data}>
                    <CartesianGrid stroke="#2a2f3a" strokeDasharray="3 3" />
                    <XAxis dataKey="time" stroke="#8b93a7" fontSize={11} />
                    <YAxis stroke="#8b93a7" fontSize={11} allowDecimals />
                    <Tooltip contentStyle={{ background: '#161b26', border: '1px solid #2a2f3a' }} />
                    <Legend />
                    <Line type="monotone" dataKey="receivedPerSec" name="Received" stroke="#4fc3f7" dot={false} strokeWidth={2} />
                    <Line type="monotone" dataKey="acceptedPerSec" name="Accepted" stroke="#66bb6a" dot={false} strokeWidth={2} />
                    <Line type="monotone" dataKey="blockedPerSec" name="Blocked"  stroke="#ef5350" dot={false} strokeWidth={2} />
                </LineChart>
            </ResponsiveContainer>
        </div>
    );
}