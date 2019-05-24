package org.mozilla.rocket.content.news

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context

class NewsViewModelFactory private constructor(
    private val repository: FakeNewsCategoryRepository
) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            return NewsViewModel(LoadNewsCategoryUseCase(repository)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile
        private var INSTANCE: NewsViewModelFactory? = null

        @JvmStatic
        fun getInstance(context: Context): NewsViewModelFactory? =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: NewsViewModelFactory(FakeNewsCategoryRepository(context.applicationContext)).also { INSTANCE = it }
            }
    }
}