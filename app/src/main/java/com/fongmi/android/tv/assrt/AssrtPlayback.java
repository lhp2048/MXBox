package com.fongmi.android.tv.assrt;

public final class AssrtPlayback {

    private static String owner = "";
    private static String title = "";
    private static String episode = "";

    private AssrtPlayback() {
    }

    public static synchronized void update(String who, String nextTitle, String nextEpisode) {
        owner = who == null ? "" : who;
        title = nextTitle == null ? "" : nextTitle;
        episode = nextEpisode == null ? "" : nextEpisode;
    }

    public static synchronized void clear(String who) {
        if (who != null && who.equals(owner)) {
            owner = "";
            title = "";
            episode = "";
        }
    }

    public static synchronized Snapshot get() {
        return new Snapshot(!owner.isEmpty(), title, episode);
    }

    public static final class Snapshot {

        public final boolean playing;
        public final String title;
        public final String episode;

        Snapshot(boolean playing, String title, String episode) {
            this.playing = playing;
            this.title = title;
            this.episode = episode;
        }
    }
}
