export interface InstanceMetrics {
    instanceID: string;
    host: string;
    port: number;
    activeConnections: number;
}

export interface ServiceMetrics {
    serviceName: string;
    instanceCount: number;
    instances: InstanceMetrics[];
    busiestInstance: string | null;
    busiestInstanceConnections: number;
}

export interface GatewaySummary {
    totalRequests: number;
    acceptedRequests: number;
    blockedRequests: Record<string, number>;
    services: ServiceMetrics[];
}

export interface TrafficPoint {
    time: string;
    receivedPerSec: number;
    acceptedPerSec: number;
    blockedPerSec: number;
}