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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class JavaFXMain extends Application {

    private CPUThread cpuThread;
    private GraphicsContext gc;
    private Stage stage;
    private Canvas canvas;

    @Override
    public void start(Stage stage) throws URISyntaxException {
        this.stage = stage;
        var root = new VBox();
        canvas = new Canvas(640, 320);
        gc = canvas.getGraphicsContext2D();

        MenuBar menuBar = createMenu();

        root.getChildren().add(menuBar);
        root.getChildren().add(canvas);

        menuBar.useSystemMenuBarProperty();

        var scene = new Scene(root, Color.BLACK);

        stage.setTitle("Remcoders CHIP8 Emulator");
        stage.setScene(scene);
        stage.show();

        loadRom(Path.of(ClassLoader.getSystemResource("roms/15PUZZLE").toURI()));
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
        loadRom(pathToRom);
    }

    private Path selectRom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fileChooser.showOpenDialog(stage).toPath();
    }

    private void loadRom(Path pathToRom) {
        if (cpuThread != null && cpuThread.isRunning()) {
            cpuThread.stop();
        }
        try {
            cpuThread = new CPUThread(gc, pathToRom);
            canvas.setOnKeyPressed(keyEvent -> cpuThread.keyEventHandler(keyEvent));
            Thread thread = new Thread(cpuThread);
            thread.start();
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

    @Override
    public void stop() throws Exception {
        cpuThread.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
