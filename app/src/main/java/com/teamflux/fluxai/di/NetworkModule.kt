package com.teamflux.fluxai.di

import android.content.Context
import com.teamflux.fluxai.network.WebhookService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideWebhookService(@ApplicationContext context: Context): WebhookService = WebhookService(context)
}
