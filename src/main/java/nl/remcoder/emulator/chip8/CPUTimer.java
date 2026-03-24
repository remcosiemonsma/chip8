package nl.remcoder.emulator.chip8;

import java.util.Timer;
import java.util.TimerTask;

public class CPUTimer {
    private Timer cpuTimer = new Timer();
    private final CPU cpu;
    private final AudioPlayer audioPlayer;

    public CPUTimer(CPU cpu, AudioPlayer audioPlayer) {
        this.cpu = cpu;
        this.audioPlayer = audioPlayer;
    }

    public void start() {
        cpuTimer = new Timer();
        cpuTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cpu.emulateCycle();
                if (cpu.getSoundTimer() > 0) {
                    audioPlayer.playSound();
                } else {
                    audioPlayer.stopSound();
                }
            }
        }, 0, 10);
    }
    
    public void stop() {
        cpuTimer.cancel();
    }
}
