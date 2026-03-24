package nl.remcoder.emulator.chip8;

import javax.sound.sampled.*;
import java.io.IOException;

public class AudioPlayer {
    private final Clip beepAudio;

    public AudioPlayer() {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(ClassLoader.getSystemResource("Beep.wav"));
            beepAudio = AudioSystem.getClip();
            beepAudio.open(audioStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void playSound() {
        if (!beepAudio.isRunning()) {
            beepAudio.setFramePosition(0);
            beepAudio.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stopSound() {
        if (beepAudio.isRunning()) {
            beepAudio.stop();
        }
    }
}
