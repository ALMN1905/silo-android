package org.siloserver.silo.common.player.audio

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Device-local switch that turns audio passthrough off for every encoded
 * format the app can decode itself, so the audio reaches the audio processors
 * (dialogue boost) as PCM instead of going to the TV or receiver as a
 * bitstream. Off by default, which keeps the platform's own passthrough
 * handling. It applies from the next audio track the player configures.
 */
class AudioPassthroughPreference(private val prefs: SharedPreferences? = null) {

    private val _passthroughDisabled = MutableStateFlow(prefs?.getBoolean(PREF_DISABLED, false) ?: false)

    /** True while passthrough is turned off. */
    val passthroughDisabled: StateFlow<Boolean> = _passthroughDisabled

    fun setPassthroughDisabled(disabled: Boolean) {
        prefs?.edit()?.putBoolean(PREF_DISABLED, disabled)?.apply()
        _passthroughDisabled.value = disabled
    }

    private companion object {
        const val PREF_DISABLED = "passthrough_disabled"
    }
}
