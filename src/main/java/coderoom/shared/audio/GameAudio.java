package coderoom.shared.audio;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/** Plays bundled PCM audio on a worker so opening a mixer never blocks Swing. */
public final class GameAudio {
    public enum Music { MENU, STUDY, ENGINE, GREENHOUSE }
    public enum Cue { CLICK, PAPER, METAL, SNAP, CART, LOCKED, SOLVED, ESCAPED, TIMEOUT }

    private static final Executor PLAYER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "game-audio");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<Cue, Clip> EFFECTS = new EnumMap<>(Cue.class);

    private static volatile boolean musicEnabled   = true;
    private static volatile boolean effectsEnabled = true;
    private static Music current;
    private static Clip loop;

    private GameAudio() { }

    public static boolean musicEnabled() { return musicEnabled; }

    public static boolean effectsEnabled() { return effectsEnabled; }

    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        queue(GameAudio::updateMusic);
    }

    public static void setEffectsEnabled(boolean enabled) {
        effectsEnabled = enabled;
        if (!enabled) { queue(() -> EFFECTS.values().stream().filter(clip -> clip != null).forEach(Clip::stop)); }
    }

    public static void playMusic(Music music) {
        queue(() -> {
            if (current == music) { return; }
            if (loop != null) { loop.close(); }
            loop    = null;
            current = music;
            updateMusic();
        });
    }

    public static void playRoom(long mapId) {
        playMusic(mapId == 3 ? Music.ENGINE : mapId == 4 ? Music.GREENHOUSE : Music.STUDY);
    }

    public static void stopMusic() { playMusic(null); }

    public static void play(Cue cue) {
        if (!effectsEnabled) { return; }
        queue(() -> {
            if (!effectsEnabled) { return; }
            if (!EFFECTS.containsKey(cue)) { EFFECTS.put(cue, open(cue)); }
            Clip clip = EFFECTS.get(cue);
            if (clip == null || clip.isRunning()) { return; }
            clip.setFramePosition(0);
            clip.start();
        });
    }

    private static void updateMusic() {
        if (!musicEnabled) {
            if (loop != null) { loop.stop(); }
        } else if (current != null) {
            if (loop == null) { loop = open(current); }
            if (loop != null && !loop.isRunning()) { loop.loop(Clip.LOOP_CONTINUOUSLY); }
        }
    }

    private static void queue(Runnable action) {
        if (!GraphicsEnvironment.isHeadless()) { PLAYER.execute(action); }
    }

    private static Clip open(Enum<?> asset) {
        String path = "/audio/" + asset.name().toLowerCase(java.util.Locale.ROOT) + ".wav";
        var resource = GameAudio.class.getResource(path);
        if (resource == null) { return null; }
        Clip clip = null;
        try (var stream = AudioSystem.getAudioInputStream(resource)) {
            clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException exception) {
            if (clip != null) { clip.close(); }
            return null;
        }
    }
}
