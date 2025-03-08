package com.luckyzero.tacotrainer.di

import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.platform.DefaultClock
import com.luckyzero.tacotrainer.repositories.TimerRepository
import com.luckyzero.tacotrainer.service.TimerRunner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationBinderModule {
    @Binds
    abstract fun provideTimerRepository(timerRunner: TimerRunner) : TimerRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ApplicationProviderModule {
    @Provides
    fun provideClock() : ClockInterface {
        return DefaultClock
    }
}