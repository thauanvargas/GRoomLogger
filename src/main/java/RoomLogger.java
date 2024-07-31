import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.GAsync;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.IntStream;

@ExtensionInfo(
        Title = "Room Logger",
        Description = "The Best Habbo Chat Logger",
        Version = "1.3.5",
        Author = "Thauan"
)

public class RoomLogger extends ExtensionForm implements Initializable {
    private static final Logger logger = Logger.getLogger(RoomLogger.class.getName());
    public static Stage primaryStage;
    public CheckBox logLocationCheckbox;
    public TextField customLocationNameTextField;
    public TextArea consoleTextArea;
    public Label infoLabel;
    public Label customNameLabel;
    public CheckBox logEntersLeavesCheckbox;
    public CheckBox logChatCheckbox;
    public CheckBox logChatBotsCheckbox;
    public CheckBox logUserActionsCheckbox;
    public Label webhookInfoLabel;
    public CheckBox enableWebhookCheckbox;
    public Button testWebhookCheckbox;
    public TextField webhookUrlTextField;
    public CheckBox logChatWebhookCheckbox;
    public CheckBox logEntersLeavesWebhookCheckbox;
    public CheckBox logLocationWebhookCheckbox;
    public CheckBox mentionWhispersWebhookCheckbox;
    public TextField discordUsernamesTextField;
    public CheckBox mentionLocationsWebhookCheckbox;
    public CheckBox hideWhispersWebhookCheckbox;
    public CheckBox disableLogWithHabboCheckbox;
    final Webhook webhook = new Webhook();
    public Label usernameLabel;
    public ListView<String> locationListView;
    public TextArea consoleLogLocations;
    public Button clearAllLogLocations;
    public Button removeLogLocation;
    public Label yourUserNameLabel;
    public Tab locationsTab;
    public Label clientVersionLabel;
    public Button focusButton;
    public ListView<String> logOnlyRoomListView;
    public CheckBox logOnlyRooms;
    private OriginsInterceptor originsInterceptor;
    private FlashInterceptor flashInterceptor;
    String roomOwner;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
        originsInterceptor = new OriginsInterceptor(this);
        flashInterceptor = new FlashInterceptor(this);
        setupCache();

        if (!webhookUrlTextField.getText().isEmpty()) {
            webhook.setWebHookUrl(webhookUrlTextField.getText());
            webhook.setDiscordUsernames(discordUsernamesTextField.getText());
            webhook.setupWebhook();
        }

        logUserActionsCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logUserActions", logUserActionsCheckbox.isSelected())
        );

        logChatBotsCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logChatBots", logChatBotsCheckbox.isSelected())
        );

        logChatCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logChat", logChatCheckbox.isSelected())
        );

        logLocationCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logLocation", logLocationCheckbox.isSelected())
        );

        logEntersLeavesCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logEntersLeaves", logEntersLeavesCheckbox.isSelected())
        );

        enableWebhookCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("enableWebhook", enableWebhookCheckbox.isSelected())
        );

        logChatWebhookCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logChatWebhook", logChatWebhookCheckbox.isSelected())
        );

        logEntersLeavesWebhookCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logEntersLeavesWebhook", logEntersLeavesWebhookCheckbox.isSelected())
        );

        logLocationWebhookCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("logLocationWebhook", logLocationWebhookCheckbox.isSelected())
        );

        logLocationCheckbox.selectedProperty().addListener(observable -> Platform.runLater(() -> {
            if(logLocationCheckbox.isSelected()) {
                infoLabel.setText("Please add a Custom Name (optional) and choose a Square in the room.");
            }
        }));

        mentionWhispersWebhookCheckbox.selectedProperty().addListener(observable -> {
                    Cacher.put("mentionWhispersWebhook", mentionWhispersWebhookCheckbox.isSelected());
                    if (!isMention()) {
                        discordUsernamesTextField.setDisable(true);
                    }
                }
        );

        mentionLocationsWebhookCheckbox.selectedProperty().addListener(observable -> {
                    Cacher.put("mentionLocationsWebhook", mentionLocationsWebhookCheckbox.isSelected());
                    if (!isMention()) {
                        discordUsernamesTextField.setDisable(true);
                    }
                }
        );

        hideWhispersWebhookCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("hideWhispersWebhook", hideWhispersWebhookCheckbox.isSelected())
        );

        discordUsernamesTextField.setOnKeyPressed((keyEvent) -> {
            Cacher.put("discordUsernames", discordUsernamesTextField.getText());
            webhook.setDiscordUsernames(discordUsernamesTextField.getText());
        });

        disableLogWithHabboCheckbox.selectedProperty().addListener(observable -> {
            if(Objects.equals(habboUserName, "")) {
                disableLogWithHabboCheckbox.setSelected(!disableLogWithHabboCheckbox.isSelected());
                return;
            }
            Cacher.put("disabledLog" + habboUserName, disableLogWithHabboCheckbox.isSelected());
        }
        );

    }

    protected String host;
    protected int habboId = -1;
    protected int habboIndex = -1;
    protected List<Player> playerList = new ArrayList<>();
    protected List<Location> locationList = new ArrayList<>();
    protected GAsync gAsync;
    public static String habboUserName;
    public boolean roomLoaded = false;
    public boolean initialEntryOnRoom = false;
    public String roomName = "";
    public int roomId = -1;
    public boolean webhookEnabled = false;
    public boolean loggerSetuped = false;
    public boolean disabled = false;
    public static boolean isOrigins = false;


    @Override
    protected void onStartConnection() {
        consoleTextArea.setEditable(false);
        logLocationCheckbox.setDisable(true);
        customLocationNameTextField.setDisable(true);
        customNameLabel.setDisable(true);
        logChatCheckbox.setSelected(true);
        logEntersLeavesCheckbox.setSelected(true);

        gAsync = new GAsync(this);
    }

    @Override
    protected void onShow() {
        if(!isOrigins) {
            new Thread(() -> {
                sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
            }).start();
            Platform.runLater(() -> {
                logOnlyRooms.setVisible(false);
                logOnlyRoomListView.setVisible(false);
                clientVersionLabel.setText("Flash");
            });
        }else {
            new Thread(() -> {
                sendToServer(new ShockPacketOutgoing("{out:INFORETRIEVE}"));
            }).start();
            Platform.runLater(() -> {
                clientVersionLabel.setText("Origins");
                logChatBotsCheckbox.setVisible(false);
                locationsTab.setDisable(true);
                logUserActionsCheckbox.setVisible(false);
                mentionLocationsWebhookCheckbox.setDisable(true);
                logLocationWebhookCheckbox.setDisable(true);
            });
        }
        System.out.println("> https://www.youtube.com/watch?v=dQw4w9WgXcQ <");
    }

    @Override
    protected void initExtension() {

        onConnect((host, port, APIVersion, versionClient, client) -> {
            this.host = host.substring(5, 7);

            if (Objects.equals(versionClient, "SHOCKWAVE")) {
                isOrigins = true;
            }
        });

        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            habboId = hMessage.getPacket().readInteger();
            habboUserName = hMessage.getPacket().readString();
            if(habboUserName != null && !loggerSetuped) {
                Platform.runLater(() -> {
                    usernameLabel.setText(habboUserName);
                });
                System.setProperty("java.util.logging.SimpleFormatter.format", "RoomLogger: %5$s%n");
                RoomLogger.setupFileLogger();
                loggerSetuped = true;

                new Thread(() -> {
                    JSONObject cache = Cacher.getCacheContents();
                    disableLogWithHabboCheckbox.setSelected(cache.optBoolean("disableLogWithHabbo" + habboUserName));

                    if(disableLogWithHabboCheckbox.isSelected()) {
                        disabled = true;
                    }
                }).start();


            }
        });

        intercept(HMessage.Direction.TOCLIENT, "GetGuestRoomResult", flashInterceptor::onGetGuestRoomResult);

        intercept(HMessage.Direction.TOCLIENT, "Users", flashInterceptor::onUsers);

        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", flashInterceptor::onUserUpdate);

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", flashInterceptor::onSetupLocation);

        intercept(HMessage.Direction.TOCLIENT, "UserRemove", flashInterceptor::onUserRemove);

        intercept(HMessage.Direction.TOCLIENT, "Chat", flashInterceptor::onChat);

        intercept(HMessage.Direction.TOCLIENT, "Shout", flashInterceptor::onChat);

        intercept(HMessage.Direction.TOCLIENT, "Whisper", flashInterceptor::onChat);

        intercept(HMessage.Direction.TOCLIENT, "Dance", flashInterceptor::onDance);

        intercept(HMessage.Direction.TOCLIENT, "Expression", flashInterceptor::onExpression);

        intercept(HMessage.Direction.TOCLIENT, "CHAT", originsInterceptor::onChat);

        intercept(HMessage.Direction.TOCLIENT, "CHAT_2", originsInterceptor::onChat);

        intercept(HMessage.Direction.TOCLIENT, "CHAT_3", originsInterceptor::onChat);

        intercept(HMessage.Direction.TOCLIENT, "USERS", originsInterceptor::onUsersOrigin);

        intercept(HMessage.Direction.TOCLIENT, "FLATINFO", originsInterceptor::onFlatInfo);

        intercept(HMessage.Direction.TOCLIENT, "ROOM_READY", originsInterceptor::onRoomReady);

        intercept(HMessage.Direction.TOCLIENT, "LOGOUT", originsInterceptor::onUserRemove);

        intercept(HMessage.Direction.TOCLIENT, "STATUS", originsInterceptor::onStatus);

        intercept(HMessage.Direction.TOCLIENT, "USER_OBJ", originsInterceptor::onUserObject);

        intercept(HMessage.Direction.TOCLIENT, "ITEMS", originsInterceptor::onItems);


    }

    static String getLocationLog(Player player, Location location, String currentDateTime, boolean left, boolean sitted) {
        String log = "[Location] Player > " + player.getName() + " < ";

        if (left) {
            log += "left position of ";
        } else {
            log += sitted ? "sitted in " : "stands in ";
        }

        if (!location.getCustomName().isEmpty()) {
            log += location.getCustomName();
        } else {
            log += "(" + location.getCoordX() + ", " + location.getCoordY() + ")";
        }

        log += " at " + currentDateTime;
        return log;
    }

    protected Location getLocation(int x, int y) {
        return locationList.stream().filter(loc -> loc.getCoordX() == x && loc.getCoordY() == y).findFirst().orElse(null);
    }

    protected Player findPlayerById(int id) {
        return playerList.stream().filter(player -> player.getId() == id).findFirst().orElse(null);
    }

    protected Player findPlayerByUserName(String userName) {
        return playerList.stream().filter(player -> Objects.equals(player.getName(), userName)).findFirst().orElse(null);
    }

    protected Player findPlayerByIndex(int index) {
        return playerList.stream().filter(player -> player.getIndex() == index).findFirst().orElse(null);
    }

    public static void logToFile(String message) {
        logger.info(message);
    }

    static void setupFileLogger() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateTime = dateFormat.format(currentDate);
        Logger logger = Logger.getLogger(RoomLogger.class.getName());

        String logFolderPath = "RoomLoggerByThauan/";

        File logFolder = new File(logFolderPath);
        if (!logFolder.exists()) {
            if (logFolder.mkdirs()) {
                System.out.println("Folder created: " + logFolderPath);
            } else {
                System.err.println("Failed to create folder: " + logFolderPath);
            }
        }

        try {
            FileHandler fileHandler = new FileHandler( logFolderPath + (isOrigins ? "origins-" : "") + habboUserName + "-" + currentDateTime + "-log.txt", 0, 1, true);
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);

            logger.addHandler(fileHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void testWebhookClick(ActionEvent actionEvent) {
        webhook.setWebHookUrl(webhookUrlTextField.getText());
        webhook.setDiscordUsernames(discordUsernamesTextField.getText());
        webhook.setupWebhook();
        boolean webhookIsOk = webhook.testWebhook();
        if (webhookIsOk) {
            Cacher.put("webhookUrl", webhookUrlTextField.getText());
            Platform.runLater(() -> {
                webhookInfoLabel.setText("Sent a message successfuly! You can enable the Webhook now");
            });
        } else {
            Platform.runLater(() -> {
                webhookInfoLabel.setText("The Webhook provided doesn't seem valid, google how to create a webhook!");
            });
        }
    }

    public void toggleWebhookClick(ActionEvent actionEvent) {
        if (webhook.getClient() != null && enableWebhookCheckbox.isSelected()) {
            webhookEnabled = true;
            webhook.setWebHookUrl(webhookUrlTextField.getText());
            Cacher.put("webhookUrl", webhookUrlTextField.getText());
            Platform.runLater(() -> {
                webhookInfoLabel.setText("Webhook is enabled.");
            });
        } else {
            Platform.runLater(() -> {
                if (webhookUrlTextField.getText().isEmpty()) {
                    webhookInfoLabel.setText("Please copy the webhook url to the field");
                } else {
                    webhookInfoLabel.setText("Please click TEST to test the Webhook first!");
                }
            });
            webhookEnabled = false;
            enableWebhookCheckbox.setSelected(false);
        }
    }

    public void hideWhispersClick(ActionEvent actionEvent) {
        if (mentionWhispersWebhookCheckbox.isSelected()) {
            Platform.runLater(() -> {
                mentionWhispersWebhookCheckbox.setSelected(false);
            });

        }
    }

    public void onClickMentions(ActionEvent actionEvent) {
        if (isMention()) {
            discordUsernamesTextField.setDisable(false);
            hideWhispersWebhookCheckbox.setSelected(false);
        } else {
            discordUsernamesTextField.setDisable(true);
        }
    }

    public boolean isMention() {
        return mentionWhispersWebhookCheckbox.isSelected() || mentionLocationsWebhookCheckbox.isSelected();
    }


    private void setupCache() {
        File extDir = null;
        try {
            extDir = (new File(RoomLogger.class.getProtectionDomain().getCodeSource().getLocation().toURI())).getParentFile();
            if (extDir.getName().equals("Extensions")) {
                extDir = extDir.getParentFile();
            }
        } catch (URISyntaxException ignored) {
        }

        Cacher.setCacheDir(extDir + File.separator + "Cache");
        loadCache();
    }


    private void loadCache() {
        JSONObject cache = Cacher.getCacheContents();

        logUserActionsCheckbox.setSelected(cache.optBoolean("logUserActions"));
        logLocationCheckbox.setSelected(cache.optBoolean("logLocation"));
        logChatCheckbox.setSelected(cache.optBoolean("logChat"));
        logChatBotsCheckbox.setSelected(cache.optBoolean("logChatBots"));
        logEntersLeavesCheckbox.setSelected(cache.optBoolean("logEntersLeaves"));

        logChatWebhookCheckbox.setSelected(cache.optBoolean("logChatWebhook"));
        logLocationWebhookCheckbox.setSelected(cache.optBoolean("logLocationWebhook"));
        logEntersLeavesWebhookCheckbox.setSelected(cache.optBoolean("logEntersLeavesWebhook"));
        enableWebhookCheckbox.setSelected(cache.optBoolean("enableWebhook"));
        hideWhispersWebhookCheckbox.setSelected(cache.optBoolean("hideWhispersWebhook"));

        mentionLocationsWebhookCheckbox.setSelected(cache.optBoolean("mentionLocationsWebhook"));
        mentionWhispersWebhookCheckbox.setSelected(cache.optBoolean("mentionWhispersWebhook"));

        JSONArray jsonArray = cache.optJSONArray("logOnlyRooms" + (isOrigins ? "Origins" : "Flash"));
        if (jsonArray != null) {
            List<String> roomList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                roomList.add(jsonArray.optString(i));
            }

            Platform.runLater(() -> {
                logOnlyRoomListView.getItems().setAll(roomList);
            });
        } else {
            Platform.runLater(() -> {
                logOnlyRoomListView.getItems().clear();
            });
        }

        if (enableWebhookCheckbox.isSelected()) {
            webhookEnabled = true;
        }

        webhookUrlTextField.setText(cache.optString("webhookUrl"));
        discordUsernamesTextField.setText(cache.optString("discordUsernames"));

        if (isMention()) {
            discordUsernamesTextField.setDisable(false);
        } else {
            discordUsernamesTextField.setDisable(true);
        }

    }


    public void disableLogWithHabbo(ActionEvent actionEvent) {
        if(disableLogWithHabboCheckbox.isSelected()) {
            disabled = true;
        }else {
            disabled = false;
        }
    }

    public void openTutorial(ActionEvent mouseEvent) throws IOException {
        Desktop.getDesktop().browse(URI.create("https://www.iorad.com/player/2103034/Discord---How-to-copy-webhook-URL-#_"));
    }

    public void clearAllLogLocations(ActionEvent actionEvent) {
        locationList.clear();
        Cacher.put("logLocation" + roomId, null);
        Platform.runLater(() -> {
            locationListView.getItems().clear();
        });
    }

    public void removeLogLocation(ActionEvent actionEvent) {
        if(locationListView.getSelectionModel().isEmpty()) {
            Platform.runLater(() -> {
                infoLabel.setText("Select a Location from the list first in order to remove.");
            });
            return;
        }

        String selectedItem = locationListView.getSelectionModel().getSelectedItem();
        String[] parts = selectedItem.split("\\) ");
        String coordinates = parts[0].substring(1); // Remove the opening parenthesis
        String customLocName = parts[1];

        String[] coords = coordinates.split(", ");
        int x = Integer.parseInt(coords[0]);
        int y = Integer.parseInt(coords[1]);

        OptionalInt indexOpt = IntStream.range(0, locationList.size())
                .filter(i -> locationList.get(i).getCoordX() == x && locationList.get(i).getCoordY() == y && locationList.get(i).getCustomName().equals(customLocName))
                .findFirst();

        if (indexOpt.isPresent()) {
            int index = indexOpt.getAsInt();
            locationList.remove(index);
        } else {
            System.out.println("Location not found!");
        }

        locationListView.getItems().remove(selectedItem);
        saveLocationsToCache();
    }

    void saveLocationsToCache() {
        JSONArray jsonLocs = new JSONArray();
        for (Location loc : locationList) {
            JSONObject jsonLoc = new JSONObject();
            jsonLoc.put("name", loc.getCustomName());
            jsonLoc.put("x", loc.getCoordX());
            jsonLoc.put("y", loc.getCoordY());
            jsonLocs.put(jsonLoc);
        }

        Cacher.put("logLocation" + roomId, jsonLocs);
    }

    public void updateTextArea(TextArea textArea, String newText) {
        ScrollPane scrollPane = (ScrollPane) textArea.lookup(".scroll-pane");
        double scrollVvalue = 0;
        double scrollVmax = 0;
        boolean atBottom = false;

        if (scrollPane != null) {
            scrollVvalue = scrollPane.getVvalue();
            scrollVmax = scrollPane.getVmax();
            atBottom = Double.compare(scrollVvalue, scrollVmax) == 0;
        }

        if (atBottom) {
            textArea.appendText(newText);
        } else {
            String currentText = textArea.getText();
            textArea.setText(currentText + newText);

            final double finalScrollVvalue = scrollVvalue;
            Platform.runLater(() -> {
                if (scrollPane != null) {
                    scrollPane.setVvalue(finalScrollVvalue);
                }
            });
        }
    }


    public void focusMode() {
        Pane originalParent = (Pane) consoleTextArea.getParent();

        int originalIndex = originalParent.getChildren().indexOf(consoleTextArea);


        if (originalParent != null) {
            originalParent.getChildren().remove(consoleTextArea);
        }

        VBox root = new VBox(consoleTextArea);
        VBox.setVgrow(consoleTextArea, Priority.ALWAYS);
        Scene scene = new Scene(root, 800, 600);
        Stage newStage = new Stage();
        newStage.setScene(scene);
        newStage.setAlwaysOnTop(true);
        newStage.initModality(Modality.WINDOW_MODAL);
        newStage.initOwner(primaryStage);
        focusButton.setDisable(true);

        javafx.scene.image.Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
        newStage.getIcons().add(icon);
        newStage.setTitle("FOCUS MODE - Room Logger by Thauan");

        newStage.setOnCloseRequest(e -> {
            if (originalParent != null) {
                originalParent.getChildren().add(originalIndex, consoleTextArea);
                focusButton.setDisable(false);
            }
        });

        newStage.show();
    }

    public void clickLogOnlyRoom(ActionEvent event) {
        if(logOnlyRooms.isSelected()) {
            AtomicReference<JSONArray> jsonArray = new AtomicReference<>(new JSONArray());
            if(roomId != -1) {
                Platform.runLater(() -> {
                    logOnlyRoomListView.getItems().add(String.valueOf(roomId));
                    logOnlyRoomListView.getItems().forEach(item -> jsonArray.get().put(item));
                    Cacher.put("logOnlyRooms" + (isOrigins ? "Origins" : "Flash"), jsonArray);
                });
            }
            if(roomOwner != null) {
                Platform.runLater(() -> {
                    if(!logOnlyRoomListView.getItems().contains(roomOwner)) {
                        logOnlyRoomListView.getItems().add(roomOwner);
                        jsonArray.set(new JSONArray());
                        logOnlyRoomListView.getItems().forEach(item -> jsonArray.get().put(item));
                        Cacher.put("logOnlyRooms" + (isOrigins ? "Origins" : "Flash"), jsonArray);
                    }
                });
            }
        }
    }

    public void clearOnlyRoomLog(ActionEvent event) {
        Platform.runLater(() -> {
            logOnlyRoomListView.getItems().clear();
            Cacher.put("logOnlyRooms" + (isOrigins ? "Origins" : "Flash"), null);
        });
    }
}
