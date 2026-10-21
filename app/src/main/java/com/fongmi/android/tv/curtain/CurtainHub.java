package com.fongmi.android.tv.curtain;

import java.io.File;
import java.util.List;

public final class CurtainHub {

    public static final class Item {
        public final String id;
        public final String kind;
        public final String label;
        public final File file;

        public Item(String id, String kind, String label, File file) {
            this.id = id;
            this.kind = kind;
            this.label = label;
            this.file = file;
        }
    }

    public interface Host {
        void apply();

        void restartMusic();

        void pushText(String text);

        void showResident(String text, int size, int color, boolean stroke, int seconds);

        void pushImage(File file);

        void clearText();

        void removePushes(List<String> ids);

        List<Item> pushes();
    }

    private static Host host;

    private CurtainHub() {
    }

    public static void attach(Host value) {
        host = value;
    }

    public static void detach(Host value) {
        if (host == value) host = null;
    }

    public static boolean active() {
        return host != null;
    }

    public static Host host() {
        return host;
    }
}
