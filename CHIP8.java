package nl.remcoder.emulator.chip8;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CHIP8 extends Application {

    CPU cpu = new CPU();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("CHIP-8 emulator");
        Group root = new Group();
        Scene scene = new Scene(root, 640, 320, Color.BLACK);
        primaryStage.setScene(scene);

        scene.addEventHandler(KeyEvent.KEY_PRESSED, key -> {
            switch (key.getCode()) {
                case Z:
                    //A
                    cpu.setKey(0xA);
                    break;
                case X:
                    //0
                    cpu.setKey(0x0);
                    break;
                case C:
                    //B
                    cpu.setKey(0xB);
                    break;
                case V:
                    //F
                    cpu.setKey(0xF);
                    break;
                case A:
                    //7
                    cpu.setKey(0x7);
                    break;
                case S:
                    //8
                    cpu.setKey(0x8);
                    break;
                case D:
                    //9
                    cpu.setKey(0x9);
                    break;
                case F:
                    //E
                    cpu.setKey(0xE);
                    break;
                case Q:
                    //4
                    cpu.setKey(0x4);
                    break;
                case W:
                    //5
                    cpu.setKey(0x5);
                    break;
                case E:
                    //6
                    cpu.setKey(0x6);
                    break;
                case R:
                    //D
                    cpu.setKey(0xD);
                    break;
                case DIGIT1:
                    //1
                    cpu.setKey(0x1);
                    break;
                case DIGIT2:
                    //2
                    cpu.setKey(0x2);
                    break;
                case DIGIT3:
                    //3
                    cpu.setKey(0x3);
                    break;
                case DIGIT4:
                    //C
                    cpu.setKey(0xC);
                    break;
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, key -> {
            cpu.setKey(-1);
        });

        cpu.initialize();

        cpu.loadRom("/home/daeron/Downloads/GAMES/GAMES/TEST/Rocket2.ch8");

        primaryStage.show();

        EventHandler<ActionEvent> gameUpdate = event ->

        {
            cpu.emulateCycle();
            root.getChildren().clear();
            boolean[][] graphics = cpu.getGraphics();
            for (int i = 0; i < graphics.length; i++) {
                boolean[] row = graphics[i];
                for (int j = 0; j < row.length; j++) {
                    if(row[j]) {
                        Rectangle pixel = new Rectangle(10, 10, Color.LIMEGREEN);
                        pixel.setX(j * 10);
                        pixel.setY(i * 10);
                        root.getChildren().add(pixel);
                    }
                }
            }
        };

        Timeline gameTimeline = new Timeline(new KeyFrame(Duration.millis(1000 / 60), gameUpdate));

        gameTimeline.setCycleCount(Animation.INDEFINITE);

        gameTimeline.play();

        Timeline soundTimeLine = new Timeline(new KeyFrame(Duration.millis(1000 / 60), event -> {
            if (cpu.getSound_timer() > 0) {
                System.out.println("Beep!");
                cpu.setSound_timer(cpu.getSound_timer() - 1);
            }
        }));

        soundTimeLine.setCycleCount(Animation.INDEFINITE);

        soundTimeLine.play();

        Timeline delayTimeline = new Timeline(new KeyFrame(Duration.millis(1000 / 60), event -> {
            if (cpu.getDelay_timer() > 0) {
                System.out.println("Delayed!");
                cpu.setDelay_timer(cpu.getDelay_timer() - 1);
            }
        }));

        delayTimeline.setCycleCount(Animation.INDEFINITE);

        delayTimeline.play();
    }
}
