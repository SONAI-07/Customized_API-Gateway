import { useEffect, useRef, useState } from 'react';
import { GatewaySummary, TrafficPoint } from '../types';

const POLL_MS = 3000;
const sumBlocked = (s: GatewaySummary) => Object.values(s.blockedRequests).reduce((a, b) => a + b, 0);

export function useGatewayMetrics() {
    const [summary, setSummary] = useState<GatewaySummary | null>(null);
    const [history, setHistory] = useState<TrafficPoint[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
    const prev = useRef<{ t: number; s: GatewaySummary } | null>(null);

    useEffect(() => {
        let alive = true;

        const poll = async () => {
            try {
                const res = await fetch('/gateway/observability/summary');
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                const data: GatewaySummary = await res.json();
                if (!alive) return;

                setSummary(data);
                setLastUpdated(new Date());
                setError(null);

                const now = Date.now();
                if (prev.current) {
                    const dtSec = (now - prev.current.t) / 1000;
                    const p = prev.current.s;
                    if (dtSec > 0) {
                        const point: TrafficPoint = {
                            time: new Date(now).toLocaleTimeString(),
                            receivedPerSec: +((data.totalRequests - p.totalRequests) / dtSec).toFixed(2),
                            acceptedPerSec: +((data.acceptedRequests - p.acceptedRequests) / dtSec).toFixed(2),
                            blockedPerSec: +((sumBlocked(data) - sumBlocked(p)) / dtSec).toFixed(2),
                        };
                        setHistory(h => [...h.slice(-59), point]); // keep last 60 points
                    }
                }
                prev.current = { t: now, s: data };
            } catch (e) {
                if (alive) setError(e instanceof Error ? e.message : String(e));
            }
        };

        poll();
        const id = setInterval(poll, POLL_MS);
        return () => { alive = false; clearInterval(id); };
    }, []);

    return { summary, history, error, lastUpdated };
}