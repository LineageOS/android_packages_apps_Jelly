package org.lineageos.jelly.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.lineageos.jelly.JellyApplication
import kotlin.reflect.safeCast

class ViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            JellyApplication::class.safeCast(application)?.let {
                return FavoriteViewModel(application, it.favoriteRepository) as T
            }
            throw IllegalArgumentException("Unsupported repository type")
        }
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            JellyApplication::class.safeCast(application)?.let {
                return HistoryViewModel(application, it.historyRepository) as T
            }
            throw IllegalArgumentException("Unsupported repository type")
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
