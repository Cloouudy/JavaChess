import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;

public class MainMenuController {

    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GameView.fxml"));

    @FXML
    private void onLocalGameClick(ActionEvent event) throws IOException {


        // Nacteni FXML souboru

        Parent gameRoot = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(gameRoot));
        stage.setResizable(true);
        stage.centerOnScreen();



        System.out.println("hra spustena");
        // Tady se pak přepne scéna na herní obrazovku
    }

    @FXML
    private void onMultiplayerClick() {
        System.out.println("Otevírám nastavení multiplayeru...");
    }

    @FXML
    private void onExitClick() {
        System.exit(0);
    }
}