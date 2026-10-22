package com.letitout.app.data

import com.letitout.app.auth.AuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds abstract fun bindAuth(impl: AuthManager): AuthGateway
    @Binds abstract fun bindChat(impl: ChatRepository): ChatGateway
    @Binds abstract fun bindPreferences(impl: DataStoreUserPreferences): UserPreferences
}
