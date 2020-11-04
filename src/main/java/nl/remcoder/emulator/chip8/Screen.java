package nl.remcoder.emulator.chip8;

import javax.swing.*;
import java.awt.*;

public class Screen extends JPanel {
    private Graphics g;
    private int scale = 10; //10 pixels for each emulated-system pixel.
    private int width = 64 * scale;
    private int height = 32 * scale;
    boolean[][] graphics;

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Paints a emulated-system pixel.
     */
    public void paintPixel(boolean white, int x, int y) {
        if (white) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.BLACK);
        }

        g.fillRect(x * scale, y * scale, scale, scale);

    }

    /**
     * Paints full screen from screen memory.
     */
    private void paintFullScreen() {

        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                boolean value = graphics[y][x];
                paintPixel(value, x, y);
            }
        }
    }


    /**
     * Paints full screen from screen memory. Public.
     */
    public void paintScreen(boolean[][] graphics) {
        this.graphics = graphics;

        repaint();
    }


    /**
     * Paints the component. It has to be called through paintScreen().
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.g = g;

        // Draw background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        paintFullScreen();


        //memory.printScreen();
    }
}
