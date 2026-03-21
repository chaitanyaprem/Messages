package org.prauga.messages.feature.financial

import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import org.prauga.messages.injection.ViewModelKey

@Module
class FinancialActivityModule {

    @Provides
    @IntoMap
    @ViewModelKey(FinancialViewModel::class)
    fun provideFinancialViewModel(viewModel: FinancialViewModel): ViewModel = viewModel

}
