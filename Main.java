package nl.remcoder.emulator.chip8;

public class Main {

    public static void main(String[] args) throws Exception {
        CPU cpu = new CPU();
        cpu.initialize();

        cpu.loadRom("/home/daeron/Downloads/GAMES/GAMES/TEST/C8PIC.ch8");

        while(true) {
            cpu.emulateCycle();
        }
    }
}
