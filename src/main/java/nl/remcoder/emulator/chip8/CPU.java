package nl.remcoder.emulator.chip8;

import java.util.Arrays;
import java.util.Random;

public class CPU {
    private int opcode = 0;
    private int[] memory = new int[4096];
    private int[] registers = new int[16];
    private int I = 0;
    private int pc = 0;
    private boolean[][] graphics = new boolean[32][64];
    private int delay_timer = 0;
    private int sound_timer = 0;
    private int[] stack = new int[16];
    private int sp = 0;
    private int key = -1;

    private Random random = new Random();
    
    private int[] chip8_fontset =
            {
                    0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                    0x20, 0x60, 0x20, 0x20, 0x70, // 1
                    0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                    0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                    0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                    0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                    0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                    0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                    0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                    0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                    0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                    0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                    0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                    0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                    0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                    0xF0, 0x80, 0xF0, 0x80, 0x80  // F
            };

    public void initialize() {
        pc = 0x200;
        opcode = 0;      // Reset current opcode
        I = 0;      // Reset index register
        sp = 0;      // Reset stack pointer
        graphics = new boolean[32][64];
        stack = new int[16];
        registers = new int[16];
        memory = new int[4096];

        System.arraycopy(chip8_fontset, 0, memory, 0, 80);
    }

    public void loadRom(byte[] romdata) {
        for (int i = 0, j = 0x200; i < romdata.length; i++, j++) {
            //Data is stored as unsigned bytes, in Java everything is signed, AND-ing with 0xff effectively removes the sign
            memory[j] = romdata[i] & 0xFF;
        }

        System.out.println("Romdata:");
        for(int i = 0; i < memory.length; i++) {
            System.out.println(Integer.toHexString(i) + ": " + Integer.toHexString(memory[i]));
        }
    }

    public void emulateCycle() {
        fetchOpcode();

        System.out.println("Opcode: " + Integer.toHexString(opcode));

        switch (opcode >> 12) {
            case (0x0) -> {
                handleCase0();
                pc += 2;
            }
            case (0x1) -> jumpToAddress();
            case (0x2) -> callSubRoutine();
            case (0x3) -> {
                skipNextInstructionIfVXEqualsNN();
                pc += 2;
            }
            case (0x4) -> {
                skipNextInstructionIfVXNotEqualsNN();
                pc += 2;
            }
            case (0x5) -> {
                skipNextInstructionIfVXEqualsVY();
                pc += 2;
            }
            case (0x6) -> {
                setVXToNN();
                pc += 2;
            }
            case (0x7) -> {
                addNNToVX();
                pc += 2;
            }
            case (0x8) -> {
                handleCase8();
                pc += 2;
            }
            case (0x9) -> {
                skipNextInstructionIfVXNotEequalsVY();
                pc += 2;
            }
            case (0xA) -> {
                setIndexRegister();
                pc += 2;
            }
            case (0xB) -> {
                jumpToNNNPlusV0();
                pc += 2;
            }
            case (0xC) -> {
                setVXToRandAndNN();
                pc += 2;
            }
            case (0xD) -> {
                drawVXVY();
                pc += 2;
            }
            case (0xE) -> {
                handleCaseE();
                pc += 2;
            }
            case (0xF) -> {
                handleCaseF();
                pc += 2;
            }
        }

//        printDisplay();

        System.out.println("Register state:");
        System.out.println(Arrays.toString(registers));
    }

    private void handleCaseF() {
        switch (opcode & 0xFF) {
            case 0x07 -> storeDelayTimerInVX();
            case 0x0A -> waitForKeyPressAndStoreInVX();
            case 0x15 -> setDelayTimerToVX();
            case 0x18 -> setSoundTimerToVX();
            case 0x1E -> addVVToI();
            case 0x29 -> setIToLocationOfValueInVX();
            case 0x33 -> storeBCDInVXToMemory();
            case 0x55 -> storeV0ThroughVXInMemory();
            case 0x65 -> readMemoryIntoV0ThroughVX();
        }
    }

    private void readMemoryIntoV0ThroughVX() {
        int VX = (opcode >> 8) & 0xF;

        System.arraycopy(memory, I, registers, 0, VX + 1);
    }

    private void storeV0ThroughVXInMemory() {
        int VX = (opcode >> 8) & 0xF;

        System.arraycopy(registers, 0, memory, I, VX + 1);
    }

    private void storeBCDInVXToMemory() {
        int VX = (opcode >> 8) & 0xF;
        int value = registers[VX];

        memory[I] = value / 100; // 100 digit
        memory[I + 1] = (value % 100) / 10; // 10 digit
        memory[I + 2] = value % 10; // 1 digit
    }

    private void setIToLocationOfValueInVX() {
        int VX = (opcode >> 8) & 0xF;
        I = registers[VX] * 5;
    }

    private void addVVToI() {
        int VX = (opcode >> 8) & 0xF;
        I += registers[VX];
    }

    private void setSoundTimerToVX() {
        int VX = (opcode >> 8) & 0xF;
        sound_timer = registers[VX];
    }

    private void setDelayTimerToVX() {
        int VX = (opcode >> 8) & 0xF;
        delay_timer = registers[VX];
    }

    private void waitForKeyPressAndStoreInVX() {
        if (isAnyKeyPressed()) {
            int VX = (opcode >> 8) & 0xF;
            registers[VX] = key;
        } else {
            pc -= 2;
        }
    }

    private boolean isAnyKeyPressed() {
        return key >= 0;
    }

    private void storeDelayTimerInVX() {
        int VX = (opcode >> 8) & 0xF;
        registers[VX] = delay_timer;
    }

    private void handleCaseE() {
        switch (opcode & 0xFF) {
            case 0x9E -> skipNextInstructionIfKeyVXPressed();
            case 0xA1 -> skipNextInstructionIfKeyVXNotPressed();
        }
    }

    private void skipNextInstructionIfKeyVXNotPressed() {
        if (!isKeyVXPressed()) {
            pc += 2;
        }
    }

    private void skipNextInstructionIfKeyVXPressed() {
        if (isKeyVXPressed()) {
            pc += 2;
        }
    }

    private boolean isKeyVXPressed() {
        int VX = (opcode >> 8) & 0xF;
        return key == registers[VX];
    }

    private void drawVXVY() {
        int VX = (opcode >> 8) & 0xF;
        int VY = (opcode >> 4) & 0xF;
        int n = opcode & 0xF;
        registers[0xF] = 0;
        for(int i = I; i < I + n; i++) {
            int x = registers[VX];
            int y = registers[VY] + i - I;
            int spritebyte = memory[i];
            if ((spritebyte & 0b1) == 0b1) {
                setPixel(x + 7, y);
            }
            if ((spritebyte & 0b10) == 0b10) {
                setPixel(x + 6, y);
            }
            if ((spritebyte & 0b100) == 0b100) {
                setPixel(x + 5, y);
            }
            if ((spritebyte & 0b1000) == 0b1000) {
                setPixel(x + 4, y);
            }
            if ((spritebyte & 0b10000) == 0b10000) {
                setPixel(x + 3, y);
            }
            if ((spritebyte & 0b100000) == 0b100000) {
                setPixel(x + 2, y);
            }
            if ((spritebyte & 0b1000000) == 0b1000000) {
                setPixel(x + 1, y);
            }
            if ((spritebyte & 0b10000000) == 0b10000000) {
                setPixel(x, y);
            }
        }
    }

    private void setPixel(int x, int y) {
        while (x >= 64) {
            x -= 64;
        }
        while (x < 0) {
            x += 64;
        }
        while (y >= 32) {
            y -= 32;
        }
        while (y < 0) {
            y += 32;
        }
        boolean pixel = graphics[y][x];
        if (pixel) {
            graphics[y][x] = false;
            registers[0xF] = 1;
        } else {
            graphics[y][x] = true;
        }
    }

    private void setVXToRandAndNN() {
        int VX = (opcode >> 8) & 0xF;
        int NN = opcode & 0xFF;
        int rand = random.nextInt(NN);
        System.out.println("Random: " + rand);
        registers[VX] = rand;
    }

    private void jumpToNNNPlusV0() {
        pc = (opcode & 0xFFF) + registers[0];
    }

    private void setIndexRegister() {
        I = opcode & 0xFFF;
    }

    private void skipNextInstructionIfVXNotEequalsVY() {
        int VX = (opcode >> 8) & 0xF;
        int VY = (opcode >> 4) & 0xF;

        if(registers[VX] != registers[VY]) {
            pc += 2;
        }
    }

    private void handleCase8() {
        switch (opcode & 0xF) {
            case 0x0 -> setVXtoVY();
            case 0x1 -> setVXtoVXorVY();
            case 0x2 -> setVXtoVXandVY();
            case 0x3 -> setVXtoVXxorVY();
            case 0x4 -> addVYtoVX();
            case 0x5 -> subtractVYfromVX();
            case 0x6 -> rightShiftVX();
            case 0x7 -> VYminusVX();
            case 0xE -> leftShiftVX();
        }
    }

    private void leftShiftVX() {
        int VX = (opcode >> 8) & 0xF;
        int VY = (opcode >> 4) & 0xF;
        registers[0xF] = (registers[VY] >> 7) & 0x1; //Most significant bit
        registers[VY] <<= 1; // 8xyE - SHL Vx {, Vy}
        if (registers[VY] > 255) {
            registers[VY] -= 256;
        }
        registers[VX] = registers[VY];
    }

    private void VYminusVX() {
        int VX = (opcode >> 8) & 0xF;
        int VY = (opcode >> 4) & 0xF;
        if (registers[VX] < registers[VY]) {
            registers[0xF] = 1;
        }
        else {
            registers[0xF] = 0;
        }
        registers[VX] = registers[VY] - registers[VX]; // 8xy7 - SUBN Vx, Vy
        if (registers[VX] < 0) {
            registers[VX] += 256;
        }
    }

    private void rightShiftVX() {
        int VX = registers[opcode >> 8 & 0xF];
        int VY = registers[opcode >> 4 & 0xF];
        registers[0xF] = registers[VY] & 0x1;
        registers[VY] >>= 1;
        registers[VX] = registers[VY];
    }

    private void subtractVYfromVX() {
        if (registers[opcode >> 8 & 0xF] > registers[opcode >> 4 & 0xF]) {
            registers[0xF] = 1;
        }
        else {
            registers[0xF] = 0;
        }
        registers[opcode >> 8 & 0xF] = registers[opcode >> 8 & 0xF] - registers[opcode >> 4 & 0xF];
        if (registers[opcode >> 8 & 0xF] < 0) {
            registers[opcode >> 8 & 0xF] += 256;
        }
    }

    private void addVYtoVX() {
        registers[opcode >> 8 & 0xF] = registers[opcode >> 8 & 0xF] + registers[opcode >> 4 & 0xF];
        if (registers[opcode >> 8 & 0xF] > 255) {
            registers[0xF] = 1;
            registers[opcode >> 8 & 0xF] -= 256;
        }
        else {
            registers[0xF] = 0;
        }
    }

    private void setVXtoVXxorVY() {
        int VY = registers[opcode >> 4 & 0xF];
        int VX = registers[opcode >> 8 & 0xF];
        registers[opcode >> 8 & 0xF] = VY ^ VX;
    }

    private void setVXtoVXandVY() {
        int VY = registers[opcode >> 4 & 0xF];
        int VX = registers[opcode >> 8 & 0xF];
        registers[opcode >> 8 & 0xF] = VY & VX;
    }

    private void setVXtoVXorVY() {
        int VY = registers[opcode >> 4 & 0xF];
        int VX = registers[opcode >> 8 & 0xF];
        registers[opcode >> 8 & 0xF] = VY | VX;
    }

    private void setVXtoVY() {
        registers[opcode >> 8 & 0xF] = registers[opcode >> 4 & 0xF];
    }

    private void addNNToVX() {
        registers[opcode >> 8 & 0xF] = registers[opcode >> 8 & 0xF] + opcode & 0xFF;
    }

    private void setVXToNN() {
        registers[opcode >> 8 & 0xF] = opcode & 0xFF;
    }

    private void skipNextInstructionIfVXEqualsVY() {
        if(registers[opcode >> 8 & 0xF] == registers[opcode >> 4 & 0xF]) {
            pc += 2;
        }
    }

    private void skipNextInstructionIfVXNotEqualsNN() {
        if(registers[opcode >> 8 & 0xF] != (opcode & 0xFF)) {
            pc += 2;
        }
    }

    private void skipNextInstructionIfVXEqualsNN() {
        if(registers[opcode >> 8 & 0xF] == (opcode & 0xFF)) {
            pc += 2;
        }
    }

    private void callSubRoutine() {
        stack[++sp] = pc;
        jumpToAddress();
    }

    private void jumpToAddress() {
        pc = opcode & 0xFFF;
    }

    private void handleCase0() {
        switch(opcode) {
            case (0X00E0):
                clearScreen();
                break;
            case (0x00EE):
                returnFromSubroutine();
                break;
            default:
                break;
        }
    }

    private void returnFromSubroutine() {
        pc = stack[sp--]; //<-- possible implementation?
    }

    private void clearScreen() {
        graphics = new boolean[32][64];
    }

    private void fetchOpcode() {
        opcode = memory[pc] << 8 | memory[pc + 1];
    }

    private void printDisplay() {
        System.out.println("DisplayState:");
        for (boolean[] line : graphics) {
            for (boolean pixel : line) {
                if (pixel) {
                    System.out.print('#');
                } else {
                    System.out.print(' ');
                }
            }
            System.out.println();
        }
    }

    public boolean[][] getGraphics() {
        return graphics;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getDelay_timer() {
        return delay_timer;
    }

    public void setDelay_timer(int delay_timer) {
        this.delay_timer = delay_timer;
    }

    public int getSound_timer() {
        return sound_timer;
    }

    public void setSound_timer(int sound_timer) {
        this.sound_timer = sound_timer;
    }
}
