import React from 'react';

export default function ResellerPortalPage() {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:4000';
  return (
    <main className="min-h-screen bg-[#060B12] text-slate-100 p-8">
      <h1 className="text-2xl font-bold tracking-tight">X-RAY LINK Reseller Partner Portal</h1>
      <p className="text-sm text-slate-400 mt-1">
        Ownership-isolated reseller dashboard consuming {apiBase}/api/v1/dashboard/reseller
      </p>
    </main>
  );
}
