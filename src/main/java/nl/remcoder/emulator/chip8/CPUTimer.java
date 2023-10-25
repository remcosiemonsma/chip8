package nl.remcoder.emulator.chip8;

import java.util.Timer;
import java.util.TimerTask;

public class CPUTimer {
    private Timer cpuTimer = new Timer();
    private final CPU cpu;

    public CPUTimer(CPU cpu) {
        this.cpu = cpu;
    }

    public void start() {
        cpuTimer = new Timer();
        cpuTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cpu.emulateCycle();
            }
        }, 0, 10);
    }
    
    public void stop() {
        cpuTimer.cancel();
    }
}
