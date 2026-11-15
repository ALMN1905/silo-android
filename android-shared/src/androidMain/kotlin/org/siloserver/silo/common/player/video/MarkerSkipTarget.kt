package org.siloserver.silo.common.player.video

import androidx.annotation.StringRes
import org.siloserver.silo.common.R
import org.siloserver.silo.domain.player.MarkerKind
import org.siloserver.silo.domain.player.MarkerTimeline

enum class ManualMarkerKind { Recap, Intro, Credits }
data class MarkerSkipTarget(val kind: ManualMarkerKind, val endSeconds: Double)

/** The label of the pill or button that skips this kind of segment. */
@StringRes
fun ManualMarkerKind.skipLabelRes(): Int = when (this) {
    ManualMarkerKind.Recap -> R.string.marker_skipRecap
    ManualMarkerKind.Intro -> R.string.introSkip_pill_skip
    ManualMarkerKind.Credits -> R.string.marker_skipCredits
}

/**
 * The recap or credits occurrence playing at [position], skipped to its own
 * end. Recap wins if provider ranges overlap. An intro is a target only when
 * [includeIntro] is set, for a client whose intro has no other prompt after
 * the timed one.
 */
fun markerSkipTarget(
    position: Double,
    markers: MarkerTimeline,
    includeIntro: Boolean = false,
): MarkerSkipTarget? {
    markers.active(MarkerKind.Recap, position)?.let { return MarkerSkipTarget(ManualMarkerKind.Recap, it.end) }
    if (includeIntro) {
        markers.active(MarkerKind.Intro, position)?.let { return MarkerSkipTarget(ManualMarkerKind.Intro, it.end) }
    }
    markers.active(MarkerKind.Credits, position)?.let { return MarkerSkipTarget(ManualMarkerKind.Credits, it.end) }
    return null
}
