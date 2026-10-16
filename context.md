# NetSentinel — Master Context & Project Source of Truth

> **Single Source of Truth** for AI models, developers, and evaluators reviewing the **NetSentinel** project for the **Application Development Lab (ADL)** course.

---

## 1. Executive Summary & Course Context

- **Course:** Application Development Lab (ADL)
- **Project Name:** NetSentinel (v1.0.0-PRO)
- **Engine:** NetTrust Security & Threat Engine (v2.4)
- **Application Type:** Enterprise Mobile Network Security Auditor, Rogue AP Hunter & Forensic Incident Recorder
- **Target Platform:** Android (Min SDK 26, Target SDK 34, Compile SDK 34)
- **Language & Stack:** Kotlin 1.9.22, Android Gradle Plugin 8.2.2, Gradle 8.2, Jetpack Architecture (MVVM)
- **Primary Database Architecture:** Dual-Layer Architecture — **Supabase PostgreSQL** (Cloud Relational SQL Database via PostgREST) & **Room SQLite 2.6.1** (Local on-device cache)

---

## 2. ADL Course Roadmap & 4-Task Alignment

| Task | Course Requirement | NetSentinel Implementation Status | Detailed Features Delivered |
| :--- | :--- | :--- | :--- |
| **Task 1** | **Design of Pages & Navigation** | ✅ **COMPLETED** | • 8 fully designed and styled screens.<br>• Dynamic theme resolution supporting both **Dark Cyber Theme** and **Day Light Theme**.<br>• Single-activity 5-tab bottom navigation (`Dashboard`, `Hunt`, `Map`, `Logs`, `Settings`) plus modal/sub-flow screens. |
| **Task 2** | **Database & Network Logic** | ✅ **COMPLETED** | • Live hardware network telemetry using Android `WifiManager` and `ConnectivityManager`.<br>• Real packet socket RTT latency measurement.<br>• Algorithmic `NetTrustEngine` for real-time security scoring (0–100 scale).<br>• **Supabase PostgreSQL Cloud Database** with full SQL query demonstration in Supabase SQL Editor.<br>• **Room SQLite** local persistence.<br>• Developer Attack Simulation Sandbox for controlled vulnerability injection. |
| **Task 3** | **Multimedia, Maps, Location & Sensors** | ✅ **COMPLETED** | • **Audio Radar Synthesizer**: Low-level PCM sine-wave synthesis via Android `AudioTrack` API.<br>• **Dual Map Engines**: Hardware-accelerated 60fps Vector Hex Map (`CyberMapView`) + OpenStreetMap (`osmdroid`) fallback.<br>• **GPS Sensor Integration**: Hardware `LocationManager` latitude/longitude binding.<br>• **Camera Evidence Capture**: Android Camera Intent with secure `FileProvider`.<br>• **Forensic Report Share**: Formatted plain-text forensic report generation (`INCIDENT_REPORT_INC-XXXX.txt`). |
| **Task 4** | **AI/ML/Chatbot & Predictive Analytics** | ⏳ *Scheduled for Next Phase* | Planned for automated anomaly inference and generative attack mitigation runbooks. |

---

## 3. High-Level System Architecture

```mermaid
graph TD
    subgraph UI_Layer ["Presentation Layer (MVVM)"]
        Splash[SplashActivity]
        Dash[DashboardFragment]
        Hunt[HuntFragment]
        Map[ThreatMapFragment]
        Logs[IncidentLogsFragment]
        Detail[IncidentDetailsFragment]
        Sim[DevSimulationFragment]
        Set[SettingsFragment]
    end

    subgraph Domain_Engine ["Core Domain & Threat Engine"]
        NetTrust[NetTrustEngine]
        BaseAna[BaselineAnalyzer]
        Finger[FingerprintEngine]
        ConfCalc[ConfidenceCalculator]
        AudioSyn[AudioRadarSynthesizer]
    end

    subgraph Hardware_Network ["Hardware Telemetry Layer"]
        WifiMgr[Android WifiManager]
        ConnMgr[Android ConnectivityManager]
        SockRTT[Socket RTT Packet Collector]
        GPSMgr[Android LocationManager]
        CamMgr[Android Camera & FileProvider]
    end

    subgraph Data_Storage ["Dual-Layer Persistence Engine"]
        SupabaseCloud[Supabase PostgreSQL Cloud DB]
        PostgREST[Supabase PostgREST API Engine]
        RoomDB[Room SQLite Database]
        Repo[Incident & Audit Repositories]
    end

    Hardware_Network --> Domain_Engine
    Domain_Engine --> UI_Layer
    UI_Layer --> Repo
    Repo --> RoomDB
    Repo --> PostgREST
    PostgREST --> SupabaseCloud
```

---

## 4. Database Architecture & Schema

NetSentinel implements a **Dual-Layer SQL Database Architecture** specifically satisfying the requirement to demonstrate real SQL queries on a relational database:

### A. Supabase PostgreSQL Cloud Database
- **Platform:** Supabase (Open-Source Firebase Alternative powered by PostgreSQL)
- **API Engine:** PostgREST / OkHttp Client (`SupabaseManager.kt`)
- **Credentials:** Defined in `SupabaseConfig.kt` (`SUPABASE_URL` and `SUPABASE_ANON_KEY`)
- **Schema File:** `supabase_schema.sql` (located at root of project)

#### Table 1: `public.audit_sessions`
```sql
CREATE TABLE public.audit_sessions (
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
```

#### Table 2: `public.incidents`
```sql
CREATE TABLE public.incidents (
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
```

### B. Room SQLite Database (`2.6.1`)
- **Entities:** `IncidentEntity`, `AuditSessionEntity`
- **DAOs:** `IncidentDao`, `AuditDao`
- **Database Class:** `NetSentinelDatabase.kt` (database name: `"netsentinel_security_db"`)
- **Role:** Local zero-latency on-device SQLite database with pure SQL CRUD operations.

---

## 5. Pure SQL Queries for Evaluator Demonstration (In Supabase SQL Editor)

When presenting to professors in the **Supabase Web Dashboard ➔ SQL Editor**:

```sql
-- 1. [READ] Fetch all logged incidents sorted by latest sync
SELECT id, title, severity, threat_score, status, ssid, timestamp_formatted 
FROM public.incidents 
ORDER BY synced_at_ms DESC;

-- 2. [READ] Filter only CRITICAL security threats
SELECT id, title, threat_score, ai_analysis, security_type 
FROM public.incidents 
WHERE severity = 'CRITICAL';

-- 3. [CREATE / INSERT] Manually insert a live security breach in SQL
INSERT INTO public.incidents (id, title, timestamp_formatted, threat_score, severity, status, ssid)
VALUES ('INC-SQL-DEMO', 'Manual SQL Rogue Hotspot Injection', '2026-10-09 10:00:00', 95, 'CRITICAL', 'FLAGGED', 'EvilTwin_Lab');

-- 4. [UPDATE] Modify incident status using SQL UPDATE
UPDATE public.incidents 
SET status = 'QUARANTINED' 
WHERE id = 'INC-SQL-DEMO';

-- 5. [DELETE] Delete the record in SQL
DELETE FROM public.incidents 
WHERE id = 'INC-SQL-DEMO';
```

---

## 6. Algorithmic Threat Detection Engine (`NetTrustEngine`)

The **NetTrust Score** is a deterministic, explainable security rating on a scale of **0 to 100**:
$$\text{NetTrust Score} = 100 - \sum \text{Anomaly Deductions}$$

### Penalty Deduction Table
| Anomaly Vector | Detected Condition | Severity | Penalty | Security Justification |
| :--- | :--- | :--- | :--- | :--- |
| **Security Downgrade** | `securityType == "Open"` or `"None"` | **CRITICAL** | **−45 pts** | Unencrypted channel enables passive over-the-air packet sniffing. |
| **Rogue DNS Resolver** | Active DNS not in trusted set | **CRITICAL** | **−40 pts** | High probability of DNS hijacking / credential harvesting. |
| **Gateway ARP Drift** | Gateway IP/MAC changes from baseline | **HIGH** | **−30 pts** | Potential ARP cache poisoning / Man-In-The-Middle (MITM). |
| **RF Packet Jamming** | Packet loss > 20% | **HIGH** | **−25 pts** | Elevated loss indicative of deauthentication flood or RF interference. |
| **Latency Path Hijack** | Socket RTT > 250 ms | **MEDIUM** | **−15 pts** | Anomalous network rerouting or deep packet inspection overhead. |
| **Evil Twin AP** | Duplicate SSID with alien BSSID | **CRITICAL** | **−50 pts** | Rogue cloned access point attempting forced client re-association. |

---

## 7. Multimedia, Sensors & Custom Views (Task 3)

### 1. Audio Radar Synthesizer (`AudioRadarSynthesizer.kt`)
- Generates dynamic acoustic pings using Android's **`AudioTrack`** API in `MODE_STATIC` / `MODE_STREAM`.
- Calculates real-time 16-bit linear PCM sine waves at sample rate 44.1 kHz.
- Modulation: Frequency shifts between **440 Hz (far / low RSSI)** and **1760 Hz (near / high RSSI)**, with ping repetition intervals shrinking from 1000 ms to 100 ms as signal strength increases.

### 2. Dual Map Engine (`ThreatMapFragment.kt`)
- **Engine 1 (Primary):** `CyberMapView.kt` — Custom 60fps hardware-accelerated 2D Vector Canvas rendering an interactive radar sweep, concentric range rings, and hex AP nodes with dynamic threat aura glows.
- **Engine 2 (Secondary):** `org.osmdroid:osmdroid-android:6.1.18` — Free open-source street map displaying real GPS coordinates without requiring proprietary Google Maps billing or API keys.

### 3. Forensic Evidence & Reporting (`IncidentDetailsFragment.kt`)
- **Camera Evidence:** Captures high-resolution images via `MediaStore.ACTION_IMAGE_CAPTURE`, saving to private cache directory and resolving URIs through Android `androidx.core.content.FileProvider`.
- **Forensic Plaintext Export:** Generates formatted cryptographic incident dossiers (`INCIDENT_REPORT_INC-XXXX.txt`) and exposes them via Android's native share intent sheet.

---

## 8. Directory Structure & Key Codebase Files

```
d:/Coding/NetSentinal/
├── app/
│   ├── build.gradle.kts                 # App dependencies (OkHttp, Room, Jetpack)
│   └── src/main/
│       ├── AndroidManifest.xml          # Permissions, FileProvider, Activities
│       ├── java/com/netsentinel/app/
│       │   ├── data/model/              # Incident, AuditSession, NetworkSnapshot
│       │   ├── database/
│       │   │   ├── SupabaseConfig.kt    # Supabase URL & Anon Key configuration
│       │   │   ├── SupabaseManager.kt   # PostgREST cloud sync & SQL endpoints
│       │   │   ├── NetSentinelDatabase  # Room SQLite DB configuration
│       │   │   ├── dao/                 # IncidentDao, AuditDao (Pure SQL methods)
│       │   │   └── entity/              # IncidentEntity, AuditSessionEntity
│       │   ├── engine/
│       │   │   ├── ThreatEngine.kt      # NetTrust core scoring logic
│       │   │   ├── BaselineAnalyzer.kt  # Gateway, DNS, and cipher drift rules
│       │   │   ├── FingerprintEngine.kt # Multi-vector AP fingerprinting
│       │   │   └── ConfidenceCalculator # Numerical confidence mapping
│       │   ├── network/
│       │   │   ├── NetworkScanner.kt    # Telemetry orchestrator
│       │   │   ├── RealWifiCollector.kt # WifiManager hardware reader
│       │   │   ├── RealConnectivity    # Gateway & DNS route resolver
│       │   │   └── RealPacketCollector  # Socket RTT latency pinger
│       │   ├── repository/
│       │   │   ├── RoomIncidentRepository.kt # Dual Room SQLite + Supabase sync
│       │   │   └── RoomAuditRepository.kt    # Dual Room SQLite + Supabase sync
│       │   ├── ui/
│       │   │   ├── custom/
│       │   │   │   ├── CircularTrustScoreView.kt # Hardware-accelerated gauge
│       │   │   │   ├── CyberMapView.kt           # 60fps Vector Hex Map
│       │   │   │   └── SignalRadarMeterView.kt   # Analog RSSI sweep meter
│       │   │   ├── dashboard/   # DashboardFragment & ViewModel
│       │   │   ├── hunt/        # HuntFragment & AudioRadarSynthesizer
│       │   │   ├── map/         # ThreatMapFragment & OSM engine
│       │   │   ├── logs/        # IncidentLogsFragment & Details
│       │   │   ├── devsim/      # DevSimulationFragment (Attack sandbox)
│       │   │   └── settings/    # SettingsFragment & ViewModel
│       │   └── utils/
│       │       └── PermissionManager.kt # Fine Location, Wi-Fi & Camera permissions
│       └── res/
│           ├── layout/          # All 8 screen XML layouts
│           ├── values/colors.xml# Theme tokens (Dark Cyber vs Day Light)
│           └── xml/file_paths.xml # FileProvider paths for photo & report export
├── supabase_schema.sql          # Ready-to-run PostgreSQL schema script for Supabase
├── build.gradle.kts             # Root Gradle plugin definitions
└── context.md                   # This master source of truth document
```

---

## 9. Live Demonstration Protocols for Evaluators

### Protocol A: Real Hardware Threat Demonstration (The Open Hotspot Test)
1. Turn on a mobile hotspot from any secondary phone without a password (Security = `None` / `Open`). Name it `Campus_Guest_WiFi`.
2. Connect the test phone to this hotspot.
3. Open NetSentinel and tap **Start Audit** on the Dashboard.
4. **Observation:**
   - SSID and Gateway resolve live.
   - NetTrust Score drops to **30 / 100 (CRITICAL THREAT)**.
   - Alert flags: *"Network security broadcast is unencrypted OPEN! Data transmitted across this channel is vulnerable to eavesdropping."*
   - Incident is automatically stored in Room SQLite and synced to Supabase PostgreSQL.

### Protocol B: Developer Attack Simulation Sandbox
1. Navigate to **Settings** ➔ Tap **Open Developer Attack Simulation Sandbox**.
2. Select any vector (e.g. *Evil Twin AP Rogue Clone* or *ARP Gateway Poisoning Attempt*).
3. Tap **Inject Attack into Local Threat Database**.
4. Navigate to **Incident Logs** to inspect the generated threat report.
5. In Supabase Dashboard, open **SQL Editor** and run `SELECT * FROM incidents ORDER BY synced_at_ms DESC;` to show the newly inserted row live in PostgreSQL!
