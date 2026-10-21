package androidx.media3.exoplayer.libass;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class LibassFontConfig {

    private LibassFontConfig() {
    }

    public static void write(File output, File cacheDirectory, List<File> fontDirectories) throws IOException {
        if (output.getParentFile() != null) output.getParentFile().mkdirs();
        if (!output.exists() && !output.createNewFile()) throw new IOException("Unable to create " + output);
    }
}
