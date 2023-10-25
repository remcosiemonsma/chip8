package nl.remcoder.emulator.chip8;

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

    private final Random random = new Random();
    
    private final int[] chip8_fontset =
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

    public void reset() {
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
            memory[j] = romdata[i] & 0xFF;
        }
    }

    public void emulateCycle() {
        fetchOpcode();

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
    }

    private void handleCaseF() {
        switch (opcode & 0xFF) {
            case 0x07 -> storeDelayTimerInVX();
            case 0x0A -> waitForKeyPressAndStoreInVX();
            case 0x15 -> setDelayTimerToVX();
            case 0x18 -> setSoundTimerToVX();
            case 0x1E -> addVXToI();
            case 0x29 -> setIToLocationOfValueInVX();
            case 0x33 -> storeBCDInVXToMemory();
            case 0x55 -> storeV0ThroughVXInMemory();
            case 0x65 -> readMemoryIntoV0ThroughVX();
        }
    }

    /**
     * Opcode FX65
     * Fill registers V0 to VX inclusive with the values stored in memory starting at address I
     * I is set to I + X + 1 after operation
     * <p>
     * Copy the values stored in memory starting at the address indicated by the value in I into registers V0 through VX
     * Afterwards, set I to I + X + 1
     * <p>
     * The opposite of FX55, here we copy from memory to the registers
     */
    private void readMemoryIntoV0ThroughVX() {
        int VX = (opcode >> 8) & 0xF;

        System.arraycopy(memory, I, registers, 0, VX + 1);

        I += VX + 1;
    }

    /**
     * Opcode FX55
     * Store the values of registers V0 to VX inclusive in memory starting at address I
     * I is set to I + X + 1 after operation
     * <p>
     * Copy the values of register V0 to VX to memory, starting at the address indicated by the value in I
     * Afterwards, set I to I + X + 1
     * <p>
     * The opposite of FX65, here we copy from the register to memory
     */
    private void storeV0ThroughVXInMemory() {
        int VX = (opcode >> 8) & 0xF;

        System.arraycopy(registers, 0, memory, I, VX + 1);

        I += VX + 1;
    }

    /**
     * Opcode FX33
     * Store the binary-coded decimal equivalent of the value stored in register VX at addresses I, I + 1, and I + 2
     * <p>
     * The value in register VX contains a binary coded decimal, this needs to be stored in memory at the addresses
     * stated by value in I. This is most commonly used to print a value so a human can read it.
     */
    private void storeBCDInVXToMemory() {
        int VX = (opcode >> 8) & 0xF;
        int value = registers[VX];

        memory[I] = value / 100; // 100 digit
        memory[I + 1] = (value % 100) / 10; // 10 digit
        memory[I + 2] = value % 10; // 1 digit
    }

    /**
     * Opcode FX29
     * Set I to the memory address of the sprite data corresponding to the hexadecimal digit stored in register VX
     * <p>
     * We want to draw one of the font sprites, so we set I to the value of register VX multiplied by 5, since each sprite
     * takes 5 bytes of memory.
     */
    private void setIToLocationOfValueInVX() {
        int VX = (opcode >> 8) & 0xF;
        I = registers[VX] * 5;
    }

    /**
     * Opcode FX1E
     * Add the value stored in register VX to register I
     * <p>
     * Increment I with the value in register VX
     */
    private void addVXToI() {
        int VX = (opcode >> 8) & 0xF;
        I += registers[VX];
    }

    /**
     * Opcode FX18
     * Set the sound timer to the value of register VX
     * <p>
     * Set the sound timer to the value in register VX
     */
    private void setSoundTimerToVX() {
        int VX = (opcode >> 8) & 0xF;
        sound_timer = registers[VX];
    }

    /**
     * Opcode FX15
     * Set the delay timer to the value of register VX
     * <p>
     * Set the delay timer to the value in register VX
     */
    private void setDelayTimerToVX() {
        int VX = (opcode >> 8) & 0xF;
        delay_timer = registers[VX];
    }

    /**
     * Opcode FX0A
     * Wait for a keypress and store the result in register VX
     * <p>
     * Check if a key is pressed, if it is store the value of the key pressed in register VX, if it is not,
     * wait until it is. We do this by decrementing the program counter so.
     */
    private void waitForKeyPressAndStoreInVX() {
        if (isAnyKeyPressed()) {
            int VX = (opcode >> 8) & 0xF;
            registers[VX] = key;
        } else {
            pc -= 2;
        }
    }

    /**
     * Check if a key is pressed
     * @return true if a key is pressed, false if not
     */
    private boolean isAnyKeyPressed() {
        return key >= 0;
    }

    /**
     * Opcode FX07
     * Store the current value of the delay timer in register VX
     * <p>
     * Take the value of the delay timer and store it in register VX
     */
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

    /**
     * Opcode EXA1
     * Skip the following instruction if the key corresponding to the hex value currently stored in register VX is pressed
     * <p>
     * Check if the key designated by the value in register VX is not pressed, if it is not we skip the next instruction
     */
    private void skipNextInstructionIfKeyVXNotPressed() {
        if (!isKeyVXPressed()) {
            pc += 2;
        }
    }

    /**
     * Opcode EX9E
     * Skip the following instruction if the key corresponding to the hex value currently stored in register VX is pressed
     * <p>
     * Check if the key designated by the value in register VX is pressed, if it is we skip the next instruction
     */
    private void skipNextInstructionIfKeyVXPressed() {
        if (isKeyVXPressed()) {
            pc += 2;
        }
    }

    /**
     * Check if the key designated by the value in register VX is pressed.
     * @return true if the key is pressed, false if it is not
     */
    private boolean isKeyVXPressed() {
        int VX = (opcode >> 8) & 0xF;
        return key == registers[VX];
    }

    /**
     * Opcode DXYN
     * Draw a sprite at position VX, VY with N bytes of sprite data starting at the address stored in I
     * Set VF to 01 if any set pixels are changed to unset, and 00 otherwise
     * <p>
     * Take the sprite data stored in the memory at the address at index I for a length of N bytes and draw this sprite
     * on screen at position VX, VY.
     */
    private void drawVXVY() {
        int VX = (opcode >> 8) & 0xF;
        int VY = (opcode >> 4) & 0xF;
        int N = opcode & 0xF;
        registers[0xF] = 0;
        for(int i = I; i < I + N; i++) {
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

    /**
     * Turn the pixel at (x,y) on and set register VF to 1 if a pixel is switched from on to off
     * <p>
     * @param x the x coord of the pixel
     * @param y the y coord of the pixel
     */
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

    /**
     * Opcode CXNN
     * Set VX to a random number with a mask of NN
     * <p>
     * Generate a random number (0-255), mask it with value NN, and store the resulting value in register VX
     */
    private void setVXToRandAndNN() {
        int VX = (opcode >> 8) & 0xF;
        int NN = opcode & 0xFF;
        int rand = random.nextInt(256);
        registers[VX] = rand & NN;
    }

    /**
     * Opcode BNNN
     * Jump to address NNN + V0
     * <p>
     * Set the opcodecounter to the value NNN added with the value in register 0
     */
    private void jumpToNNNPlusV0() {
        pc = (opcode & 0xFFF) + registers[0];
    }

    /**
     * Opcode ANNN
     * Store memory address NNN in register I
     * <p>
     * Set the index to the address NNN
     */
    private void setIndexRegister() {
        I = opcode & 0xFFF;
    }

    /**
     * Opcode 9XY0
     * Skip the following instruction if the value of register VX is not equal to the value of register VY
     * <p>
     * Check if the value in register VX is not equal to the value in register VY, if it is not, skip the next instruction by
     * incrementing the program counter
     * <p>
     * Similar to 5XY0 but here we test for inequality
     */
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
            case 0x7 -> subtractVXfromVY();
            case 0xE -> leftShiftVX();
        }
    }

    /**
     * Opcode 8XYE
     * Store the value of register VY shifted left one bit in register VX
     * Set register VF to the most significant bit prior to the shift
     * VY is unchanged
     * <p>
     * Take the value from register VY, store the most significant bit in register VF (so we know if a carry occurred)
     * then left shift the value 1 bit (so, multiply by 2) and store the resulting value in register VX
     */
    private void leftShiftVX() {
        int VX = (opcode >> 8) & 0xF;
        int VY = (opcode >> 4) & 0xF;
        registers[0xF] = (registers[VY] >> 7) & 0x1; //Most significant bit
        registers[VX] = registers[VY] << 1;
        if (registers[VX] > 255) {
            registers[VX] -= 256;
        }
    }

    /**
     * Opcode 8XY7
     * Set register VX to the value of VY minus VX
     * Set VF to 00 if a borrow occurs
     * Set VF to 01 if a borrow does not occur
     * <p>
     * Take the value of VX and subtract it from the value in VY, if a borrow (i.e. an underflow) occurs we set VF to 1, if not we
     * set VF to 0
     * <p>
     * Similar to 8XY5, but we subtract VX from VY
     */
    private void subtractVXfromVY() {
        int VX = opcode >> 8 & 0xF;
        int VY = opcode >> 4 & 0xF;
        registers[VX] = registers[VY] - registers[VX];
        if (registers[VX] < 0) {
            registers[0xF] = 0;
            registers[VX] += 256;
        } else {
            registers[0xF] = 1;
        }
    }

    /**
     * Opcode 8XY6
     * Store the value of register VY shifted right one bit in register VX
     * Set register VF to the least significant bit prior to the shift
     * VY is unchanged
     * <p>
     * Take the value from register VY, store the least significant bit in register VF (so we know the value was even or odd)
     * then right shift the value 1 bit (so, divide by 2) and store the resulting value in register VX
     */
    private void rightShiftVX() {
        int VX = (opcode >> 8) & 0xF;
        int VY = (opcode >> 4) & 0xF;
        registers[0xF] = registers[VY] & 0x1;
        registers[VX] = registers[VY] >> 1;
    }

    /**
     * Opcode 8XY5
     * Subtract the value of register VY from register VX
     * Set VF to 00 if a borrow occurs
     * Set VF to 01 if a borrow does not occur
     * <p>
     * Take the value of VY and subtract it from the value in VX, if a borrow (i.e. an underflow) occurs we set VF to 1, if not we
     * set VF to 0
     * <p>
     * Similar to 8XY7 but we subtract VY from VX
     */
    private void subtractVYfromVX() {
        int VX = opcode >> 8 & 0xF;
        int VY = opcode >> 4 & 0xF;
        registers[VX] -= registers[VY];
        if (registers[VX] < 0) {
            registers[0xF] = 0;
            registers[VX] += 256;
        } else {
            registers[0xF] = 1;
        }
    }

    /**
     * Opcode 8XY4
     * Add the value of register VY to register VX
     * Set VF to 01 if a carry occurs
     * Set VF to 00 if a carry does not occur
     * <p>
     * Take the value of VY and add it to the value in VX, if a carry (i.e. an overflow) occurs we set VF to 1, if not we
     * set VF to 0
     */
    private void addVYtoVX() {
        int VX = opcode >> 8 & 0xF;
        int VY = opcode >> 4 & 0xF;
        registers[VX] = registers[VX] + registers[VY];
        if (registers[VX] > 255) {
            registers[0xF] = 1;
            registers[VX] -= 256;
        }
        else {
            registers[0xF] = 0;
        }
    }

    /**
     * Opcode 8XY3
     * Set VX to VX XOR VY
     * <p>
     * Take the value from register VX, and perform a binary XOR on it with the value from register VY and store the
     * resulting value in register VX
     */
    private void setVXtoVXxorVY() {
        int VY = opcode >> 4 & 0xF;
        int VX = opcode >> 8 & 0xF;
        registers[VX] ^= registers[VY];
    }

    /**
     * Opcode 8XY2
     * Set VX to VX AND VY
     * <p>
     * Take the value from register VX, and perform a binary AND on it with the value from register VY and store the
     * resulting value in register VX
     */
    private void setVXtoVXandVY() {
        int VX = opcode >> 8 & 0xF;
        int VY = opcode >> 4 & 0xF;
        registers[VX] &= registers[VY];
    }

    /**
     * Opcode 8XY1
     * Set VX to VX OR VY
     * <p>
     * Take the value from register VX, and perform a binary OR on it with the value from register VY and store the
     * resulting value in register VX
     */
    private void setVXtoVXorVY() {
        int VX = opcode >> 8 & 0xF;
        int VY = opcode >> 4 & 0xF;
        registers[VX] |= registers[VY];
    }

    /**
     * Opcode 8XY0
     * Store the value of register VY in register VX
     * <p>
     * Take the value stored in register VY and store it in register VX
     */
    private void setVXtoVY() {
        int VX = opcode >> 8 & 0xF;
        int VY = opcode >> 4 & 0xF;
        registers[VX] = registers[VY];
    }

    /**
     * Opcode 7XNN
     * Add the value NN to register VX
     * <p>
     * Add the value provided by NN to the value in register VX
     */
    private void addNNToVX() {
        int VX = (opcode >> 8) & 0xF;
        int NN = opcode & 0xFF;
        registers[VX] += NN;
        if (registers[VX] > 255) {
            registers[VX] -= 256;
        }
    }

    /**
     * Opcode 6XNN
     * Store number NN in register VX
     * <p>
     * Store the value provided by NN in register VX
     */
    private void setVXToNN() {
        int VX = (opcode >> 8) & 0xF;
        int NN = opcode & 0xFF;
        registers[VX] = NN;
    }

    /**
     * Opcode 5XY0
     * Skip the following instruction if the value of register VX is equal to the value of register VY
     * <p>
     * Check if the value in register VX is equal to the value in register VY, if it is, skip the next instruction by
     * incrementing the program counter
     * <p>
     * Similar to 3XNN, but now with another register for comparison
     * Similar to 9XY0, but now we test for equality
     */
    private void skipNextInstructionIfVXEqualsVY() {
        if(registers[opcode >> 8 & 0xF] == registers[opcode >> 4 & 0xF]) {
            pc += 2;
        }
    }

    /**
     * Opcode 4XNN
     * Skip the following instruction if the value of register VX does not equals NN
     * <p>
     * Check if the value in register VX is not equal to the value provided by NN, if it is not, skip the next instruction by
     * incrementing the program counter
     * <p>
     * (The reverse of 3XNN)
     */
    private void skipNextInstructionIfVXNotEqualsNN() {
        int VX = (opcode >> 8) & 0xF;
        int NN = opcode & 0xFF;
        if(registers[VX] != NN) {
            pc += 2;
        }
    }

    /**
     * Opcode 3XNN
     * Skip the following instruction if the value of register VX equals NN
     * <p>
     * Check if the value in register VX is equal to the value provided by NN, if it is, skip the next instruction by
     * incrementing the program counter
     * (The reverse of 4XNN)
     */
    private void skipNextInstructionIfVXEqualsNN() {
        int VX = (opcode >> 8) & 0xF;
        int NN = opcode & 0xFF;
        if(registers[VX] == NN) {
            pc += 2;
        }
    }

    /**
     * Opcode 2NNN
     * Execute subroutine starting at address NNN
     * <p>
     * Address NNN is a memory address, store the current program counter in the program counter stack and then
     * set the program counter to NNN
     */
    private void callSubRoutine() {
        stack[++sp] = pc;
        jumpToAddress();
    }

    /**
     * Opcode 1NNN
     * Jump to adress NNN
     * <p>
     * Address NNN is a memory address, just set the program counter to this value and continue from there
     */
    private void jumpToAddress() {
        pc = opcode & 0xFFF;
    }

    private void handleCase0() {
        switch (opcode) {
            case (0X00E0) -> clearScreen();
            case (0x00EE) -> returnFromSubroutine();
            default -> {
            }
        }
    }

    /**
     * Opcode 00EE
     * Return from subroutine
     * <p>
     * The CHIP-8 uses a stack pointer to keep track of subroutines, when a subroutine ends simply set the program counter
     * back to the value of the program counter when the subroutine was called.
     */
    private void returnFromSubroutine() {
        pc = stack[sp--]; //<-- possible implementation?
    }

    /**
     * Opcode 00E0
     * Clear the screen
     * <p>
     * As it says, clear the screen. Easiest done by just creating a new boolean grid :)
     */
    private void clearScreen() {
        graphics = new boolean[32][64];
    }

    private void fetchOpcode() {
        opcode = memory[pc] << 8 | memory[pc + 1];
    }

    public boolean[][] getGraphics() {
        return graphics;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void decrementDelayTimer() {
        if (delay_timer > 0) {
            delay_timer--;
        }
    }

    public int getSound_timer() {
        return sound_timer;
    }

    public void setSound_timer(int sound_timer) {
        this.sound_timer = sound_timer;
    }
}
