import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.ThemedExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class RoomLoggerLauncher extends ThemedExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/RoomLogger.fxml"));
        Parent root = loader.load();
        root.getStyleClass().add("root");

        primaryStage.setTitle("Room Logger by Thauan");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(true);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.getIcons().add(new Image("icon.png"));
        primaryStage.getScene().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm());
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(450);

        return loader.getController();
    }

    @Override
    protected String getTitle() {
        return "RoomLogger v" + RoomLogger.class.getAnnotation(ExtensionInfo.class).Version();
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("/RoomLogger.fxml");
    }

    public static void main(String[] args) {
        runExtensionForm(args, RoomLoggerLauncher.class);
    }
}