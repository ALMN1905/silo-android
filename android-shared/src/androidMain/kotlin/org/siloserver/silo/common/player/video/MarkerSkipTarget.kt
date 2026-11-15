package org.siloserver.silo.common.player.video

import org.siloserver.silo.domain.player.MarkerKind
import org.siloserver.silo.domain.player.MarkerTimeline

enum class ManualMarkerKind { Recap, Intro, Credits }
data class MarkerSkipTarget(val kind: ManualMarkerKind, val endSeconds: Double)

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
