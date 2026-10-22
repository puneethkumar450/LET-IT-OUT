package com.letitout.app.reminder

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
