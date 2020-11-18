package nl.remcoder.emulator.chip8;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaFXMain extends Application {

    private CPUThread cpuThread;

    @Override
    public void start(Stage stage) throws URISyntaxException, IOException {
        var root = new VBox();
        var canvas = new Canvas(640, 320);
        var gc = canvas.getGraphicsContext2D();

        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open");
        fileMenu.getItems().add(openMenuItem);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        //Adding action on the menu item
        openMenuItem.setOnAction(event -> {
            //Opening a dialog box
            fileChooser.showOpenDialog(stage);
        });

        MenuBar menuBar = new MenuBar(fileMenu);

        root.getChildren().add(menuBar);
        root.getChildren().add(canvas);

        menuBar.useSystemMenuBarProperty();

        var scene = new Scene(root, Color.BLACK);

        stage.setTitle("Lines");
        stage.setScene(scene);
        stage.show();

        cpuThread = new CPUThread(gc);
        Thread thread = new Thread(cpuThread);

        thread.start();
    }

    @Override
    public void stop() throws Exception {
        cpuThread.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }

    private static class CPUThread implements Runnable {

        private final CPU cpu;
        private final GraphicsContext graphicsContext;
        private boolean running = true;

        public CPUThread(GraphicsContext graphicsContext) throws URISyntaxException, IOException {
            this.graphicsContext = graphicsContext;
            cpu = new CPU();
            cpu.initialize();

            cpu.loadRom(Files.readAllBytes(Path.of(ClassLoader.getSystemResource("roms/15PUZZLE").toURI())));
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            while(running) {
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
    }
}
