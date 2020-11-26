package nl.remcoder.emulator.chip8;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;

public class CPUTimer extends AnimationTimer {

    private final CPU cpu;
    private final GraphicsContext graphicsContext;
    private final AudioClip beepAudio;

    public CPUTimer(GraphicsContext graphicsContext, CPU cpu) {
        this.graphicsContext = graphicsContext;
        this.cpu = cpu;
        beepAudio = new AudioClip(ClassLoader.getSystemResource("Beep.wav").toString());
        beepAudio.setCycleCount(AudioClip.INDEFINITE);
    }

    @Override
    public void handle(long now) {
        cpu.emulateCycle();
        paintScreen(cpu.getGraphics(), graphicsContext);
        if (!beepAudio.isPlaying() && cpu.getSound_timer() > 0) {
            beepAudio.play();
        }
        if (beepAudio.isPlaying() && cpu.getSound_timer() == 0) {
            beepAudio.stop();
        }
        if (cpu.getSound_timer() > 0) {
            cpu.setSound_timer(cpu.getSound_timer() - 1);
        }
    }

    private void paintScreen(boolean[][] graphics, GraphicsContext graphicsContext) {
        graphicsContext.clearRect(0, 0, 640, 320);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                boolean value = graphics[y][x];
                paintPixel(value, x, y, graphicsContext);
            }
        }
    }

    private void paintPixel(boolean white, int x, int y, GraphicsContext graphicsContext) {
        if (white) {
            graphicsContext.setFill(Color.LIMEGREEN);
        } else {
            graphicsContext.setFill(Color.BLACK);
        }

        graphicsContext.fillRect(x * 10, y * 10, 10, 10);
    }
}
