-- ============================================================================
-- NetSentinel — Supabase PostgreSQL Database Schema
-- Run this script in the Supabase SQL Editor (Dashboard -> SQL Editor -> New Query)
-- ============================================================================

-- 1. Create table for Audit Sessions
CREATE TABLE IF NOT EXISTS public.audit_sessions (
    id TEXT PRIMARY KEY,
    start_time_ms BIGINT NOT NULL,
    end_time_ms BIGINT NOT NULL,
    trust_score INT NOT NULL,
    ssid TEXT,
    bssid TEXT,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    synced_at_ms BIGINT
);

-- 2. Create table for Incident Threat Records
CREATE TABLE IF NOT EXISTS public.incidents (
    id TEXT PRIMARY KEY,
    audit_session_id TEXT REFERENCES public.audit_sessions(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    timestamp_formatted TEXT NOT NULL,
    threat_score INT NOT NULL,
    severity TEXT NOT NULL,
    status TEXT NOT NULL,
    ai_analysis TEXT,
    ssid TEXT,
    bssid TEXT,
    gateway_ip TEXT,
    security_type TEXT,
    gps_coordinates TEXT,
    photo_uri TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    synced_at_ms BIGINT
);

-- 3. Create Indexes for fast querying
CREATE INDEX IF NOT EXISTS idx_incidents_severity ON public.incidents(severity);
CREATE INDEX IF NOT EXISTS idx_incidents_threat_score ON public.incidents(threat_score DESC);
CREATE INDEX IF NOT EXISTS idx_audit_sessions_created ON public.audit_sessions(created_at DESC);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.audit_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.incidents ENABLE ROW LEVEL SECURITY;

-- 5. Create Permissive Policies for App Demonstration (Allow Anon Read/Write)
CREATE POLICY "Allow public read audit_sessions" ON public.audit_sessions
    FOR SELECT USING (true);

CREATE POLICY "Allow public insert audit_sessions" ON public.audit_sessions
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow public update audit_sessions" ON public.audit_sessions
    FOR UPDATE USING (true);

CREATE POLICY "Allow public delete audit_sessions" ON public.audit_sessions
    FOR DELETE USING (true);

CREATE POLICY "Allow public read incidents" ON public.incidents
    FOR SELECT USING (true);

CREATE POLICY "Allow public insert incidents" ON public.incidents
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Allow public update incidents" ON public.incidents
    FOR UPDATE USING (true);

CREATE POLICY "Allow public delete incidents" ON public.incidents
    FOR DELETE USING (true);

-- ============================================================================
-- SQL CRUD QUERIES (TO RUN & SHOW PROFESSOR IN SUPABASE SQL EDITOR)
-- ============================================================================

-- [READ 1] Select all incidents ordered by latest sync
-- SELECT id, title, severity, threat_score, status, ssid, timestamp_formatted 
-- FROM public.incidents 
-- ORDER BY synced_at_ms DESC;

-- [READ 2] Select only CRITICAL security breaches
-- SELECT id, title, threat_score, ai_analysis 
-- FROM public.incidents 
-- WHERE severity = 'CRITICAL';

-- [CREATE] Manually insert a threat incident in pure SQL
-- INSERT INTO public.incidents (id, title, timestamp_formatted, threat_score, severity, status, ssid)
-- VALUES ('INC-SQL-DEMO', 'Manual SQL Rogue Hotspot Injection', '2026-10-09 10:00:00', 95, 'CRITICAL', 'FLAGGED', 'EvilTwin_Lab');

-- [UPDATE] Update incident status in SQL
-- UPDATE public.incidents 
-- SET status = 'QUARANTINED' 
-- WHERE id = 'INC-SQL-DEMO';

-- [DELETE] Delete demo record in SQL
-- DELETE FROM public.incidents 
-- WHERE id = 'INC-SQL-DEMO';
