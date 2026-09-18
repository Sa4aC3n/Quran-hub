package com.example.data.provider

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.model.Reciter

/**
 * Reciter Image & Artwork Provider.
 * Provides fallback resources and notification artwork URIs.
 */
object ReciterImageProvider {

    @DrawableRes
    val DEFAULT_PLACEHOLDER_RES: Int = R.drawable.ic_launcher_foreground

    @DrawableRes
    fun getDrawableRes(reciterId: String? = null, reciterName: String? = null): Int {
        return DEFAULT_PLACEHOLDER_RES
    }

    @DrawableRes
    fun getDrawableRes(reciter: Reciter): Int {
        return DEFAULT_PLACEHOLDER_RES
    }

    fun getImageUrl(reciter: Reciter): String? {
        return null
    }

    /**
     * Resolves an Artwork URI formatted as android.resource://[packageName]/[resId]
     * for system notification and MediaSession display.
     */
    fun getArtworkUri(context: Context, reciterId: String, reciterName: String? = null): Uri {
        return Uri.parse("android.resource://${context.packageName}/$DEFAULT_PLACEHOLDER_RES")
    }

    fun isCustomArtworkAvailable(reciterId: String, reciterName: String? = null): Boolean {
        return false
    }
}
