package org.mozilla.rocket.content.news.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.content.news.domain.LoadNewsLanguagesUseCase

class NewsSettingsViewModelFactory constructor(
    private val repository: NewsSettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsSettingsViewModel::class.java)) {
            return NewsSettingsViewModel(LoadNewsLanguagesUseCase(repository)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}