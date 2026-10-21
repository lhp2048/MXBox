package androidx.media3.common.text;

import androidx.media3.common.TrackSelectionOverride;

public final class SubtitleSelectionState {

    public static final SubtitleSelectionState EMPTY = new SubtitleSelectionState(null, null);

    public final TrackSelectionOverride primarySelection;
    public final TrackSelectionOverride activeSecondarySelection;
    public final TrackSelectionOverride requestedSecondarySelection;
    public final java.util.List<TrackSelectionOverride> secondaryCandidates;
    public final boolean secondaryPromotedToPrimary;

    public SubtitleSelectionState(TrackSelectionOverride primarySelection, TrackSelectionOverride activeSecondarySelection) {
        this.primarySelection = primarySelection;
        this.activeSecondarySelection = activeSecondarySelection;
        this.requestedSecondarySelection = null;
        this.secondaryCandidates = java.util.List.of();
        this.secondaryPromotedToPrimary = false;
    }

    public boolean isPrimary(TrackSelectionOverride selection) {
        return selection != null && selection.equals(primarySelection);
    }

    public boolean isActiveSecondary(TrackSelectionOverride selection) {
        return selection != null && selection.equals(activeSecondarySelection);
    }
}
