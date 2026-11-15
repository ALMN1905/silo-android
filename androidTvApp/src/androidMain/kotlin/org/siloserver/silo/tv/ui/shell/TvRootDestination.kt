package org.siloserver.silo.tv.ui.shell

/**
 * Identifies which top-level destination the menu bar selects/highlights:
 * `Home`, one tab per visible [TvLibraryTabType], For You, then `Calendar`.
 * Search and the profile avatar sit outside the tabs, and Requests lives in
 * the profile menu. Mirrors tvOS `TVRootDestination`.
 */
sealed class TvRootDestination {
    /** Curated landing surface. Always the first tab. */
    data object Home : TvRootDestination()

    /** A library content-type tab (Movies / Series / Music / Audiobooks). */
    data class LibraryType(val type: TvLibraryTabType) : TvRootDestination()

    /**
     * One tab per library (the opt-in "tab per library" top menu): [type] picks
     * the route and the cascade's sections, [libraryId] the scope.
     */
    data class Library(
        val type: TvLibraryTabType,
        val libraryId: Int,
        val name: String,
    ) : TvRootDestination()

    /** Personal recommendations ("For You") — tvOS `.recommendations`. */
    data object ForYou : TvRootDestination()

    /** Upcoming releases by week. */
    data object Calendar : TvRootDestination()
}
