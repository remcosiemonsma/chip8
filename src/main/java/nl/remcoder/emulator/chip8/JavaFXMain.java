package nl.remcoder.emulator.chip8;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaFXMain extends Application {

    private CPU cpu;
    private Stage stage;
    private CPUTimer cpuTimer;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        var root = new VBox();
        var canvas = new Canvas(640, 320);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        InputStream inputStream = ClassLoader.getSystemResource("Background.png").openStream();

        Image image = new Image(inputStream);

        gc.drawImage(image, 0, 0);

        MenuBar menuBar = createMenu();

        root.getChildren().add(menuBar);
        root.getChildren().add(canvas);

        menuBar.useSystemMenuBarProperty();

        var scene = new Scene(root, Color.BLACK);

        cpu = new CPU();

        stage.addEventHandler(KeyEvent.KEY_PRESSED, this::keyPressedHandler);
        stage.addEventHandler(KeyEvent.KEY_RELEASED, this::keyReleasedHandler);

        stage.setTitle("Remcoders CHIP8 Emulator");
        stage.setScene(scene);
        stage.show();

        cpuTimer = new CPUTimer(canvas.getGraphicsContext2D(), cpu);
    }

    private MenuBar createMenu() {
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open");
        openMenuItem.setOnAction(event -> selectAndLoadRom());
        fileMenu.getItems().add(openMenuItem);
        return new MenuBar(fileMenu);
    }

    private void selectAndLoadRom() {
        Path pathToRom = selectRom();
        if (pathToRom != null) {
            cpuTimer.stop();
            loadRom(pathToRom);
            cpuTimer.start();
        }
    }

    private Path selectRom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            return file.toPath();
        } else {
            return null;
        }
    }

    private void loadRom(Path pathToRom) {
        try {
            cpu.reset();
            cpu.loadRom(Files.readAllBytes(pathToRom));
        } catch (IOException e) {
            e.printStackTrace();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);

            VBox vbox = new VBox(new Text("An error occured:\n" + e.getMessage()), new Button("Ok."));
            vbox.setAlignment(Pos.CENTER);
            vbox.setPadding(new Insets(15));

            dialogStage.setScene(new Scene(vbox));
            dialogStage.show();
        }
    }

    private void keyPressedHandler(KeyEvent keyEvent) {
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

    private void keyReleasedHandler(KeyEvent keyEvent) {
        cpu.setKey(-1);
    }

    public static void main(String[] args) {
        launch();
    }
}
