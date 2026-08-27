package idv.wennyli.bloodpressurelog.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import idv.wennyli.bloodpressurelog.data.repository.AuthRepository
import idv.wennyli.bloodpressurelog.data.repository.AuthRepositoryImpl
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepository
import idv.wennyli.bloodpressurelog.data.repository.BloodPressureRepositoryImpl
import idv.wennyli.bloodpressurelog.utils.ResourceProvider
import idv.wennyli.bloodpressurelog.utils.ResourceProviderImpl
import idv.wennyli.bloodpressurelog.utils.SnackbarController
import idv.wennyli.bloodpressurelog.utils.SnackbarControllerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun provideResourceProvider(impl: ResourceProviderImpl): ResourceProvider

    @Binds
    @Singleton
    abstract fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun provideBloodPressureRepository(impl: BloodPressureRepositoryImpl): BloodPressureRepository

    @Binds
    @Singleton
    abstract fun provideSnackbarController(impl: SnackbarControllerImpl): SnackbarController
}