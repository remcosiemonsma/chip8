package nl.remcoder.emulator.chip8;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        CPU cpu = new CPU();
        cpu.initialize();

        cpu.loadRom(Files.readAllBytes(Path.of(ClassLoader.getSystemResource("roms/CONNECT4").toURI())));


        JFrame f = new JFrame("CHIP-8 emulator (Remcoder)");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem load = new JMenuItem("Load ROM");
        load.addActionListener(e -> {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.showOpenDialog(f);
        });
        file.add(load);
        JMenuItem reset = new JMenuItem("Reset");
        file.add(reset);
        mb.add(file);

        Screen screen =  new Screen();
        f.add(screen);
        f.pack();
        f.setVisible(true);
        f.setJMenuBar(mb);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            switch (e.getID()) {
                case KeyEvent.KEY_PRESSED:
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_1 -> cpu.setKey(0x1);
                        case KeyEvent.VK_2 -> cpu.setKey(0x2);
                        case KeyEvent.VK_3 -> cpu.setKey(0x3);
                        case KeyEvent.VK_4 -> cpu.setKey(0xC);
                        case KeyEvent.VK_Q -> cpu.setKey(0x4);
                        case KeyEvent.VK_W -> cpu.setKey(0x5);
                        case KeyEvent.VK_E -> cpu.setKey(0x6);
                        case KeyEvent.VK_R -> cpu.setKey(0xD);
                        case KeyEvent.VK_A -> cpu.setKey(0x7);
                        case KeyEvent.VK_S -> cpu.setKey(0x8);
                        case KeyEvent.VK_D -> cpu.setKey(0x9);
                        case KeyEvent.VK_F -> cpu.setKey(0xE);
                        case KeyEvent.VK_Z -> cpu.setKey(0xA);
                        case KeyEvent.VK_X -> cpu.setKey(0x0);
                        case KeyEvent.VK_C -> cpu.setKey(0xB);
                        case KeyEvent.VK_V -> cpu.setKey(0xF);
                    }
                    break;
                case KeyEvent.KEY_RELEASED:
                    cpu.setKey(-1);
            }
            return true;
        });


        while(true) {
            cpu.emulateCycle();
            screen.paintScreen(cpu.getGraphics());
            Thread.sleep(10);
        }
    }
}
