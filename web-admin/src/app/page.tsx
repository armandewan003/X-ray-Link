import React from 'react';

export interface OverviewStats {
  totalUsers: number;
  activeUsers: number;
  expiredUsers: number;
  totalResellers: number;
  activeResellers: number;
  totalServers: number;
  onlineServers: number;
  activeConnections: number;
  uploadTrafficBytes: number;
  downloadTrafficBytes: number;
  totalCredits: number;
}

export default async function SuperAdminDashboardPage() {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:4000';
  return (
    <main className="min-h-screen bg-[#060B12] text-slate-100 p-8">
      <h1 className="text-2xl font-bold tracking-tight">X-RAY LINK Super Admin Console</h1>
      <p className="text-sm text-slate-400 mt-1">
        Connected to authoritative backend endpoint: {apiBase}/api/v1/dashboard/overview
      </p>
    </main>
  );
}
