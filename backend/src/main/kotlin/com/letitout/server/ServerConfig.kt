package com.letitout.server

internal data class ServerConfig(
    val production: Boolean,
    val chatEnabled: Boolean,
    val projectId: String?,
    val groqApiKey: String?,
    val groqModel: String,
    val simliApiKey: String?,
    val simliFaceId: String?,
    val simliBackgroundIds: Map<String, String>,
    val port: Int,
    val perUserHourlyLimit: Int,
    val globalHourlyLimit: Int,
    /** Groq-hosted safety classifier; null when GROQ_SAFETY_MODEL=off. */
    val groqSafetyModel: String? = DEFAULT_SAFETY_MODEL,
    val trial: TrialRules = TrialRules()
) {
    companion object {
        const val DEFAULT_SAFETY_MODEL = "openai/gpt-oss-20b"

        fun fromEnvironment(env: Map<String, String> = System.getenv()): ServerConfig {
            fun integer(name: String, default: Int): Int = env[name]?.let {
                requireNotNull(it.toIntOrNull()) { "$name must be an integer" }
            } ?: default
            val appEnv = env["APP_ENV"] ?: "local"
            require(appEnv == "local" || appEnv == "production") { "APP_ENV must be local or production" }
            val production = appEnv == "production"
            val chatEnabled = env["CHAT_ENABLED"]?.let {
                require(it == "true" || it == "false") { "CHAT_ENABLED must be true or false" }
                it == "true"
            } ?: !production
            val port = integer("PORT", 8080)
            // Two back-to-back 15-minute chats fit in an hour; the daily trial limit is the main cap.
            val perUserLimit = integer("CHAT_USER_HOURLY_LIMIT", 40)
            val globalLimit = integer("CHAT_GLOBAL_HOURLY_LIMIT", 300)
            require(port in 1..65535) { "PORT must be between 1 and 65535" }
            require(perUserLimit in 1..1000) { "CHAT_USER_HOURLY_LIMIT must be between 1 and 1000" }
            require(globalLimit in 1..100_000) { "CHAT_GLOBAL_HOURLY_LIMIT must be between 1 and 100000" }
            val projectId = env["FIREBASE_PROJECT_ID"]?.trim()?.takeIf { it.isNotEmpty() }
            val apiKey = env["GROQ_API_KEY"]?.trim()?.takeIf { it.isNotEmpty() }
            val model = env["GROQ_MODEL"]?.trim()?.takeIf { it.isNotEmpty() } ?: "openai/gpt-oss-120b"
            val safetyModel = env["GROQ_SAFETY_MODEL"]?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_SAFETY_MODEL
            val simliApiKey = env["SIMLI_API_KEY"]?.trim()?.takeIf { it.isNotEmpty() }
            val simliFaceId = (env["SIMLI_FACE_ID_PILOT"] ?: env["SIMLI_FACE_ID_WOMAN_ADULT"])
                ?.trim()?.takeIf { it.isNotEmpty() }
            val simliBackgroundIds = listOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK")
                .mapNotNull { mood ->
                    env["SIMLI_BACKGROUND_ID_$mood"]?.trim()?.takeIf { it.isNotEmpty() }?.let { mood to it }
                }.toMap()
            val trial = TrialRules(
                chatsPerDay = integer("TRIAL_CHATS_PER_DAY", 3),
                chatSeconds = integer("TRIAL_CHAT_MINUTES", 15) * 60L,
                liveSeconds = integer("TRIAL_LIVE_SECONDS", 180).toLong(),
                liveEverySeconds = integer("TRIAL_LIVE_EVERY_DAYS", 7) * 24 * 3600L,
                liveAttemptsPerWindow = integer("TRIAL_LIVE_ATTEMPTS", 3),
                zone = java.time.ZoneId.of(env["TRIAL_TIMEZONE"]?.trim()?.takeIf { it.isNotEmpty() } ?: "Asia/Kolkata")
            )
            require(trial.chatsPerDay in 1..50) { "TRIAL_CHATS_PER_DAY must be between 1 and 50" }
            require(trial.chatSeconds in 60..7200) { "TRIAL_CHAT_MINUTES must be between 1 and 120" }
            require(trial.liveSeconds in 30..1800) { "TRIAL_LIVE_SECONDS must be between 30 and 1800" }
            require(trial.liveEverySeconds in 0..(30 * 24 * 3600L)) { "TRIAL_LIVE_EVERY_DAYS must be between 0 and 30" }
            require(trial.liveAttemptsPerWindow in 1..10) { "TRIAL_LIVE_ATTEMPTS must be between 1 and 10" }
            if (production) {
                require(projectId != null) { "FIREBASE_PROJECT_ID is required in production" }
                if (chatEnabled) require(apiKey != null) { "GROQ_API_KEY is required when production chat is enabled" }
            }
            return ServerConfig(production, chatEnabled, projectId, apiKey, model, simliApiKey, simliFaceId, simliBackgroundIds, port, perUserLimit, globalLimit,
                safetyModel.takeUnless { it.equals("off", ignoreCase = true) }, trial)
        }
    }
}
