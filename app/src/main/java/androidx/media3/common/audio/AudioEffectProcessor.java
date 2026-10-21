package androidx.media3.common.audio;

import java.nio.ByteBuffer;

public final class AudioEffectProcessor extends BaseAudioProcessor {

    public void setParameters(AudioEffectParameters parameters, boolean softwareEqualizer) {
    }

    public void resetParameters() {
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining == 0) return;
        ByteBuffer output = replaceOutputBuffer(remaining);
        output.put(inputBuffer);
        output.flip();
    }
}
