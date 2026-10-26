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
    val globalHourlyLimit: Int
) {
    companion object {
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
            val perUserLimit = integer("CHAT_USER_HOURLY_LIMIT", 20)
            val globalLimit = integer("CHAT_GLOBAL_HOURLY_LIMIT", 300)
            require(port in 1..65535) { "PORT must be between 1 and 65535" }
            require(perUserLimit in 1..1000) { "CHAT_USER_HOURLY_LIMIT must be between 1 and 1000" }
            require(globalLimit in 1..100_000) { "CHAT_GLOBAL_HOURLY_LIMIT must be between 1 and 100000" }
            val projectId = env["FIREBASE_PROJECT_ID"]?.trim()?.takeIf { it.isNotEmpty() }
            val apiKey = env["GROQ_API_KEY"]?.trim()?.takeIf { it.isNotEmpty() }
            val model = env["GROQ_MODEL"]?.trim()?.takeIf { it.isNotEmpty() } ?: "openai/gpt-oss-120b"
            val simliApiKey = env["SIMLI_API_KEY"]?.trim()?.takeIf { it.isNotEmpty() }
            val simliFaceId = (env["SIMLI_FACE_ID_PILOT"] ?: env["SIMLI_FACE_ID_WOMAN_ADULT"])
                ?.trim()?.takeIf { it.isNotEmpty() }
            val simliBackgroundIds = listOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK")
                .mapNotNull { mood ->
                    env["SIMLI_BACKGROUND_ID_$mood"]?.trim()?.takeIf { it.isNotEmpty() }?.let { mood to it }
                }.toMap()
            if (production) {
                require(projectId != null) { "FIREBASE_PROJECT_ID is required in production" }
                if (chatEnabled) require(apiKey != null) { "GROQ_API_KEY is required when production chat is enabled" }
            }
            return ServerConfig(production, chatEnabled, projectId, apiKey, model, simliApiKey, simliFaceId, simliBackgroundIds, port, perUserLimit, globalLimit)
        }
    }
}
