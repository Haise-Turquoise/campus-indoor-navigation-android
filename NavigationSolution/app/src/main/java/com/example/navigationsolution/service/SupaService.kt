package com.example.navigationsolution.service

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import com.example.navigationsolution.data.UserData
import io.github.jan.supabase.postgrest.postgrest


object SupabaseService {
    private const val SUPABASE_URL = "https://gtexwiesoxnfbbmwptwl.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd0ZXh3aWVzb3huZmJibXdwdHdsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDA5NDkwNjYsImV4cCI6MjA1NjUyNTA2Nn0.XR7vV72Ir65w7z2qpFIYpGHGsaNEa6UgfJKHn-LFces"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        // 只安装 Postgrest 模块【Only install Postgrest module】
        install(Postgrest)
    }
    suspend fun fetchAllUsers(): List<UserData> {
        return client.postgrest["users"]
            .select().decodeList<UserData>()
    }

}


