package com.luckyzero.tacotrainer.di

import com.luckyzero.tacotrainer.platform.ClockInterface
import com.luckyzero.tacotrainer.platform.DefaultClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent

@Module
@InstallIn(ServiceComponent::class)
abstract class ServiceModule {
    @Binds
    abstract fun bindClock(
        clock: DefaultClock
    ) : ClockInterface
}