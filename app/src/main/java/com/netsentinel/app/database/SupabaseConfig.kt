package com.netsentinel.app.database

/**
 * Global configuration for Supabase PostgreSQL backend.
 * Replace SUPABASE_URL and SUPABASE_ANON_KEY with your project credentials
 * found in Supabase Dashboard -> Project Settings -> API.
 */
object SupabaseConfig {
    // Live project URL from user
    var SUPABASE_URL: String = "https://ctttggaqwabqvqifdnke.supabase.co"

    // Live publishable anon key from user
    var SUPABASE_ANON_KEY: String = "sb_publishable_xEchNR9jSsz5kKANUF2mQg_4V1eIleM"

    val isConfigured: Boolean
        get() = SUPABASE_URL.startsWith("https://") &&
                !SUPABASE_URL.contains("your-project") &&
                SUPABASE_ANON_KEY != "your-anon-key-here" &&
                SUPABASE_ANON_KEY.isNotEmpty()
}
