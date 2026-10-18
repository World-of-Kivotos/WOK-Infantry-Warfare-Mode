import biniu.ogg.Packet;
import biniu.ogg.Page;
import biniu.ogg.StreamState;
import biniu.vorbis.Block;
import biniu.vorbis.Comment;
import biniu.vorbis.DspState;
import biniu.vorbis.Info;
import biniu.vorbis.VorbisEnc;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generates the downed-state tinnitus loop for WOK步战附属-倒地救援.
 *
 * <p>Pure synthesis, no third-party samples: two closely detuned high tones that beat against each
 * other (the ringing itself), a soft low-passed hiss that reads as blood pressure and room noise,
 * and a slow amplitude wobble so the loop is not a flat sine. Everything is deterministic (fixed
 * seeds), so re-running this tool reproduces the same samples and the same file.
 *
 * <p>Build and run from the repository root (JDK 17+):
 * <pre>
 *   javac -d tools/downed-audio/out -cp tools/downed-audio/lib/jVorbisEnc-0.0.2.jar \
 *         tools/downed-audio/TinnitusGen.java
 *   java -cp "tools/downed-audio/out;tools/downed-audio/lib/jVorbisEnc-0.0.2.jar" TinnitusGen \
 *         wok_downed/src/main/resources/assets/wok_downed/sounds/downed/tinnitus.ogg
 * </pre>
 * Output is 44.1 kHz mono Vorbis, matching the other WOK sounds. The encoder call pattern follows
 * the library's own {@code EncodeExample}; the only addition is feeding samples from memory.
 */
public final class TinnitusGen {
    private static final int RATE = 44100;
    private static final int READ = 1024;
    private static final float SECONDS = 4.0F;
    private static final float PRIMARY_HZ = 4180.0F;
    private static final float SECONDARY_HZ = 4196.0F;
    private static final int HISS_CUTOFF_HZ = 1250;
    private static final float WOBBLE_HZ = 0.5F;
    private static final float CROSSFADE_SECONDS = 0.35F;
    private static final long SAMPLE_SEED = 20261017L;
    private static final long STREAM_SEED = 314159265358979L;

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "tinnitus.ogg");
        float[] pcm = synthesise();
        crossfade(pcm);
        if (out.toAbsolutePath().getParent() != null) {
            Files.createDirectories(out.toAbsolutePath().getParent());
        }
        try (OutputStream stream = new BufferedOutputStream(
                new FileOutputStream(out.toFile()))) {
            encode(pcm, stream);
        }
        System.out.printf("wrote %s: %d samples @ %d Hz%n", out, pcm.length, RATE);
    }

    private static float[] synthesise() {
        int length = Math.round(RATE * SECONDS);
        float[] out = new float[length];
        Random random = new Random(SAMPLE_SEED);
        double hissState = 0.0;
        double hissCoefficient = 1.0 - Math.exp(-2.0 * Math.PI * HISS_CUTOFF_HZ / RATE);
        for (int i = 0; i < length; i++) {
            double t = i / (double) RATE;
            double primary = Math.sin(2.0 * Math.PI * PRIMARY_HZ * t);
            double secondary = Math.sin(2.0 * Math.PI * SECONDARY_HZ * t);
            // The two tones drift in and out of phase at (secondary - primary) Hz; that beat is
            // the ringing itself, and the slow wobble only rides on top of it.
            double wobble = 0.82 + 0.18 * Math.sin(2.0 * Math.PI * WOBBLE_HZ * t);
            double ring = 0.42 * (primary + secondary) * wobble;
            double white = random.nextDouble() * 2.0 - 1.0;
            hissState += hissCoefficient * (white - hissState);
            double hiss = hissState * 0.9;
            out[i] = (float) clamp((ring + hiss) * 0.45);
        }
        return out;
    }

    /**
     * Wraps the tail into the head so looping is seamless. Without this the buffer would jump from
     * full amplitude back to zero once per loop, audible as a click every four seconds.
     */
    private static void crossfade(float[] pcm) {
        int fade = Math.round(RATE * CROSSFADE_SECONDS);
        int length = pcm.length;
        for (int i = 0; i < fade; i++) {
            double t = i / (double) fade;
            // Equal-power crossfade (sin/cos) keeps perceived loudness steady across the seam.
            double head = Math.cos(t * Math.PI / 2.0);
            double tail = Math.sin(t * Math.PI / 2.0);
            pcm[i] = (float) (pcm[i] * head + pcm[length - fade + i] * tail);
        }
    }

    private static double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }

    /** Mirrors {@code EncodeExample.encode} with samples coming from {@code pcm} instead of a WAV. */
    private static void encode(float[] pcm, OutputStream out) throws IOException {
        StreamState state = new StreamState();
        Page page = new Page();
        Packet packet = new Packet();
        Info info = new Info();
        Comment comment = new Comment();
        DspState dsp = new DspState();
        Block block = new Block(dsp);
        VorbisEnc encoder = new VorbisEnc();
        boolean eos = false;

        info.init();
        // 44 kHz mono, VBR quality 0.1: plenty for a narrow-band tone and a very small file.
        int result = encoder.initVBR(info, 1, RATE, 0.1F);
        if (result != 0) {
            throw new IllegalStateException("initVBR failed: " + result);
        }
        comment.init();
        comment.addTag("ENCODER", "WOK步战附属-倒地救援 TinnitusGen");

        dsp.analysisInit(info);
        block.blockInit(dsp);
        state.init(new Random(STREAM_SEED).nextInt());

        Packet header = new Packet();
        Packet headerComm = new Packet();
        Packet headerCode = new Packet();
        dsp.analysisHeaderOut(comment, header, headerComm, headerCode);
        state.packetIn(header);
        state.packetIn(headerComm);
        state.packetIn(headerCode);
        // Ensure the audio data starts on a fresh page, as the container spec requires.
        while (state.flush(page)) {
            out.write(page.header_base, page.header, page.header_len);
            out.write(page.body_base, page.body, page.body_len);
        }

        int offset = 0;
        while (!eos) {
            int remaining = pcm.length - offset;
            if (remaining <= 0) {
                // End of stream: tell the library so it can flush the final frame.
                dsp.analysisWrote(0);
            } else {
                int count = Math.min(READ, remaining);
                float[][] buffer = dsp.analysisBuffer(READ);
                int base = dsp.pcm_current;
                System.arraycopy(pcm, offset, buffer[0], base, count);
                offset += count;
                dsp.analysisWrote(count);
            }
            if (remaining <= 0) {
                // analysisWrote(0) produced the closing block; drain it below, then stop.
            }
            while (block.analysisBlockOut()) {
                block.analysis(null);
                block.bitrateAddBlock();
                while (dsp.bitrateFlushPacket(packet)) {
                    state.packetIn(packet);
                    while (state.pageOut(page)) {
                        out.write(page.header_base, page.header, page.header_len);
                        out.write(page.body_base, page.body, page.body_len);
                        if (page.eos()) {
                            eos = true;
                        }
                    }
                }
            }
            if (remaining <= 0 && !eos) {
                break;
            }
        }

        state.clear();
        block.clear();
        dsp.clear();
        comment.clear();
        info.clear();
    }

    private TinnitusGen() {
    }
}
