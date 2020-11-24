package nl.remcoder.emulator.chip8;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class CPUThread implements Runnable {

    private final CPU cpu;
    private final GraphicsContext graphicsContext;

    private boolean running = true;

    public CPUThread(GraphicsContext graphicsContext, Path pathToRom) throws IOException {
        this.graphicsContext = graphicsContext;
        cpu = new CPU();
        cpu.initialize();

        cpu.loadRom(Files.readAllBytes(pathToRom));
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        while (running) {
            cpu.emulateCycle();
            paintScreen(cpu.getGraphics(), graphicsContext);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void paintScreen(boolean[][] graphics, GraphicsContext graphicsContext) {
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

    public void keyPressedHandler(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case DIGIT1 -> cpu.setKey(0x1);
            case DIGIT2 -> cpu.setKey(0x2);
            case DIGIT3 -> cpu.setKey(0x3);
            case DIGIT4 -> cpu.setKey(0xC);
            case Q -> cpu.setKey(0x4);
            case W -> cpu.setKey(0x5);
            case E -> cpu.setKey(0x6);
            case R -> cpu.setKey(0xD);
            case A -> cpu.setKey(0x7);
            case S -> cpu.setKey(0x8);
            case D -> cpu.setKey(0x9);
            case F -> cpu.setKey(0xE);
            case Z -> cpu.setKey(0xA);
            case X -> cpu.setKey(0x0);
            case C -> cpu.setKey(0xB);
            case V -> cpu.setKey(0xF);
        }
    }

    public void keyReleasedHandler(KeyEvent keyEvent) {
        cpu.setKey(-1);
    }
}
