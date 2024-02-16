import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.AwaitingPacket;
import gearth.extensions.extra.tools.GAsync;
import gearth.extensions.parsers.*;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@ExtensionInfo(
        Title = "Room Logger",
        Description = "The Best Habbo Chat Logger",
        Version = "1.1",
        Author = "Thauan"
)

public class RoomLogger extends ExtensionForm implements Initializable {
    private static final Logger logger = Logger.getLogger(RoomLogger.class.getName());
    public static Stage primaryStage;
    public CheckBox logLocationCheckbox;
    public CheckBox isSittedLocationCheckbox;
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
    private final Webhook webhook = new Webhook();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

        isSittedLocationCheckbox.selectedProperty().addListener(observable ->
                Cacher.put("isSitted", isSittedLocationCheckbox.isSelected())
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
            infoLabel.setText("Please choose a Square in the room, if it's where a bench stays, let 'is Sitted?' checked");
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
    protected int habboId;
    protected int habboIndex = -1;
    protected List<Player> playerList = new ArrayList<>();
    protected List<Location> locationList = new ArrayList<>();
    protected GAsync gAsync;
    public static String habboUserName;
    public boolean roomLoaded = false;
    public boolean initialEntryOnRoom = false;
    public String roomName = "";
    public boolean webhookEnabled = false;
    public boolean loggerSetuped = false;
    public boolean disabled = false;


    @Override
    protected void onStartConnection() {
        new Thread(() -> {
            sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
        }).start();

        isSittedLocationCheckbox.setSelected(true);
        consoleTextArea.setEditable(false);
        isSittedLocationCheckbox.setDisable(true);
        logLocationCheckbox.setDisable(true);
        customLocationNameTextField.setDisable(true);
        customNameLabel.setDisable(true);
        logChatCheckbox.setSelected(true);
        logEntersLeavesCheckbox.setSelected(true);

        gAsync = new GAsync(this);

    }

    @Override
    protected void onShow() {
        System.out.println("> https://www.youtube.com/watch?v=dQw4w9WgXcQ <");
    }

    @Override
    protected void initExtension() {

        onConnect((host, port, APIVersion, versionClient, client) -> {
            this.host = host.substring(5, 7);
        });

        intercept(HMessage.Direction.TOCLIENT, "UserObject", hMessage -> {
            habboId = hMessage.getPacket().readInteger();
            habboUserName = hMessage.getPacket().readString();
            if(habboUserName != null && !loggerSetuped) {
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

        intercept(HMessage.Direction.TOCLIENT, "GetGuestRoomResult", this::onGetGuestRoomResult);

        intercept(HMessage.Direction.TOCLIENT, "Users", this::onUsers);

        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", this::onUserUpdate);

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::onSetupLocation);

        intercept(HMessage.Direction.TOCLIENT, "UserRemove", this::onUserRemove);

        intercept(HMessage.Direction.TOCLIENT, "Chat", this::onChat);

        intercept(HMessage.Direction.TOCLIENT, "Shout", this::onChat);

        intercept(HMessage.Direction.TOCLIENT, "Whisper", this::onChat);

        intercept(HMessage.Direction.TOCLIENT, "Dance", this::onDance);

        intercept(HMessage.Direction.TOCLIENT, "Expression", this::onExpression);

    }

    private void onGetGuestRoomResult(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        boolean entered = hPacket.readBoolean();
        hPacket.readInteger();
        roomName = hPacket.readString(StandardCharsets.UTF_8);

        if (!entered) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentDateTime = dateFormat.format(currentDate);
            String logRoomReset = "Room " + roomName + " loaded at " + currentDateTime;
            if (webhookEnabled && !disabled) {
                webhook.sendLog(logRoomReset, null, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
            }
            if(!disabled) {
                logToFile(logRoomReset);
            }
            Platform.runLater(() -> {
                infoLabel.setText("Locations was resetted because room was reloaded.");
                consoleTextArea.appendText(logRoomReset + "\n");
            });
            roomLoaded = false;
            playerList.clear();
            locationList.clear();
        } else {
            initialEntryOnRoom = false;
        }

        if (!roomLoaded && !entered) {
            Platform.runLater(() -> {
                isSittedLocationCheckbox.setDisable(false);
                logLocationCheckbox.setDisable(false);
                customLocationNameTextField.setDisable(false);
                customNameLabel.setDisable(false);
                infoLabel.setText("Room was loaded, extension ready, any question ask in discord > thauanvargas <");
            });
            roomLoaded = true;
            initialEntryOnRoom = true;
        }

    }


    private void onChat(HMessage hMessage) {
        if (roomLoaded && logChatCheckbox.isSelected() && !disabled) {
            HPacket hPacket = hMessage.getPacket();
            int index = hPacket.readInteger();
            String message = hPacket.readString(StandardCharsets.UTF_8);
            hPacket.readInteger();
            int bubble = hPacket.readInteger();
            Player player = findPlayerByIndex(index);

            if (player != null) {
                boolean isBot = player.isBot();
                if (player.isBot() && !logChatBotsCheckbox.isSelected()) {
                    return;
                }
                Date currentDate = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                String currentDateTime = dateFormat.format(currentDate);
                String hash = this.getPacketInfoManager().getPacketInfoFromHeaderId(HMessage.Direction.TOCLIENT, hPacket.headerId()).getName();

                if (Objects.equals(hash, "Whisper")) {
                    if (bubble == 34) {
                        hash = "Wired";
                    }
                }

                String logChat = "[" + (isBot ? "BOT" : hash) + "] [" + currentDateTime + "] " + player.getName() + " : " + message;
                logToFile(logChat);
                Platform.runLater(() -> {
                    consoleTextArea.appendText(logChat + "\n");
                });
                if (webhookEnabled && logChatWebhookCheckbox.isSelected() && !isBot &&
                        !Objects.equals(hash, "Wired") && !disabled) {

                    boolean isWhisper = Objects.equals(hash, "Whisper");

                    if (isWhisper && hideWhispersWebhookCheckbox.isSelected()) {
                        return;
                    }

                    webhook.sendLog(logChat, player, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());

                }
            }
        }
    }

    private void onExpression(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        int index = hPacket.readInteger();
        int expression = hPacket.readInteger();
        Player player = findPlayerByIndex(index);

        if (player != null && logUserActionsCheckbox.isSelected() && !disabled) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentDateTime = dateFormat.format(currentDate);
            String expressionName = "";
            if (expression == 1) {
                expressionName = "Waved";
            } else if (expression == 2) {
                expressionName = "Sent a kiss";
            } else if (expression == 3) {
                expressionName = "Laugh";
            } else if (expression == 5) {
                expressionName = "Idled";
            } else if (expression == 7) {
                expressionName = "Did a Like";
            }

            if (expression != 4 && expression != 0) {
                String logChat = "[Action] " + player.getName() + " " + expressionName + " at " + currentDateTime;
                logToFile(logChat);
                Platform.runLater(() -> {
                    consoleTextArea.appendText(logChat + "\n");
                });
            }
        }
    }

    private void onDance(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        int index = hPacket.readInteger();
        int dance = hPacket.readInteger();
        Player player = findPlayerByIndex(index);

        if (player != null && logUserActionsCheckbox.isSelected() && !disabled) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentDateTime = dateFormat.format(currentDate);
            String danceName = "";
            if (dance == 1) {
                danceName = "Hap-hop";
            } else if (dance == 2) {
                danceName = "Pogo-Mogo";
            } else if (dance == 3) {
                danceName = "Duck Funk";
            } else if (dance == 4) {
                danceName = "Rollie";
            }

            String logChat = "[Action] " + player.getName() + (!danceName.isEmpty() ? " started dancing " + danceName : " stopped dancing") + " at " + currentDateTime;
            logToFile(logChat);
            Platform.runLater(() -> {
                consoleTextArea.appendText(logChat + "\n");
            });
        }
    }

    private void onUsers(HMessage hMessage) {
        new Thread(() -> {
            boolean isInitial = initialEntryOnRoom;
            if (roomLoaded) {
                try {
                    HPacket hPacket = hMessage.getPacket();
                    HEntity[] roomUsersList = HEntity.parse(hPacket);
                    for (HEntity hEntity : roomUsersList) {
                        if (hEntity.getName().equals(habboUserName)) {
                            habboIndex = hEntity.getIndex();
                        }

                        if (hEntity.getEntityType() == HEntityType.PET) {
                            continue;
                        }

                        Player player = findPlayerById(hEntity.getId());

                        if (player == null) {
                            player = new Player(hEntity.getId(), hEntity.getIndex(), hEntity.getName());
                            if (hEntity.getEntityType() == HEntityType.BOT || hEntity.getEntityType() == HEntityType.OLD_BOT) {
                                player.setBot(true);
                            } else {
                                player.setFigureId(hEntity.getFigureId());
                            }
                            player.setCoordX(-1);
                            player.setCoordY(-1);
                            playerList.add(player);
                            Date currentDate = new Date();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            String currentDateTime = dateFormat.format(currentDate);
                            String logEntered = "";
                            if (!isInitial) {
                                logEntered = "[Join] Player > " + player.getName() + " < entered the room at " + currentDateTime;
                            } else {
                                logEntered = "Player > " + player.getName() + " < is at the room on load ";
                            }
                            if (logEntersLeavesCheckbox.isSelected() && !disabled) {
                                logToFile(logEntered);
                                String finalLogEntered = logEntered;
                                Platform.runLater(() -> {
                                    consoleTextArea.appendText(finalLogEntered + "\n");
                                });
                            }
                            if (webhookEnabled && logEntersLeavesWebhookCheckbox.isSelected() && !isInitial && !disabled) {
                                webhook.sendLog(logEntered, player, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
                            }
                        } else {
                            player.setFigureId(hEntity.getFigureId());
                            player.setIndex(hEntity.getIndex());
                            player.setCoordX(-1);
                            player.setCoordY(-1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void onUserUpdate(HMessage hMessage) {
        if (roomLoaded) {
            try {
                for (HEntityUpdate hEntityUpdate : HEntityUpdate.parse(hMessage.getPacket())) {
                    HStance hStance = hEntityUpdate.getStance();
                    int currentIndex = hEntityUpdate.getIndex();
                    int currentX = hEntityUpdate.getTile().getX();
                    int currentY = hEntityUpdate.getTile().getY();
                    Player player = findPlayerByIndex(currentIndex);
                    Location location = getLocation(currentX, currentY);
                    Date currentDate = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    String currentDateTime = dateFormat.format(currentDate);

                    if (player == null) {
                        continue;
                    }

                    if (logUserActionsCheckbox.isSelected()) {
                        if (hEntityUpdate.getSign() != null) {
                            String log = "[Action] " + player.getName() + " showed sign " + hEntityUpdate.getSign() + "  at " + currentDateTime;
                            logToFile(log);
                            Platform.runLater(() -> {
                                consoleTextArea.appendText(log + "\n");
                            });
                        }
                    }

                    if (location == null) {
                        continue;
                    }

                    if (hEntityUpdate.getAction() == HAction.Move || hEntityUpdate.getAction() == HAction.Sit) {
                        if (player.getLocation() != null) {
                            String log = getLocationLog(player, player.getLocation(), currentDateTime, true);
                            logToFile(log);
                            if (webhookEnabled && logLocationWebhookCheckbox.isSelected() && !disabled) {
                                webhook.sendLog(log, player, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
                            }
                            Platform.runLater(() -> {
                                consoleTextArea.appendText(log + "\n");
                            });
                            player.setLocation(null);
                            continue;
                        }

                        player.setCoordX(currentX);
                        player.setCoordY(currentY);

                        if (!location.isSitted() || (location.isSitted() && hStance == HStance.Sit)) {
                            String log = getLocationLog(player, location, currentDateTime, false);
                            logToFile(log);
                            if (webhookEnabled && logLocationWebhookCheckbox.isSelected() && !disabled) {
                                webhook.sendLog(log, player, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
                            }
                            Platform.runLater(() -> {
                                consoleTextArea.appendText(log + "\n");
                            });
                        }
                    }

                    player.setLocation(location);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onUserRemove(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();

        String index = hPacket.readString();
        Player player = findPlayerByIndex(Integer.parseInt(index));

        if (player != null) {
            if (logEntersLeavesCheckbox.isSelected()) {
                Date currentDate = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String currentDateTime = dateFormat.format(currentDate);
                String logLeave = "[Leave] Player > " + player.getName() + " < left the room at " + currentDateTime;
                logToFile(logLeave);
                if (webhookEnabled && logEntersLeavesWebhookCheckbox.isSelected() && !disabled) {
                    webhook.sendLog(logLeave, player, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
                }
                Platform.runLater(() -> {
                    consoleTextArea.appendText(logLeave + "\n");
                });
            }
            playerList.remove(player);
        }

    }

    private static String getLocationLog(Player player, Location location, String currentDateTime, boolean left) {
        String log = "[Location] Player > " + player.getName() + " < ";

        if (left) {
            log += "left position of ";
        } else {
            if (location.isSitted()) {
                log += "sitted in ";
            } else {
                log += "stands in ";
            }
        }

        if (!location.getCustomName().isEmpty()) {
            log += location.getCustomName();
        } else {
            log += "furni " + location.getFurniId();
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

    protected Player findPlayerByIndex(int index) {
        return playerList.stream().filter(player -> player.getIndex() == index).findFirst().orElse(null);
    }

    protected void onSetupLocation(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        HPoint point = new HPoint(hPacket.readInteger(), hPacket.readInteger());

        if (logLocationCheckbox.isSelected()) {
            new Thread(() -> {
                if (customLocationNameTextField.getText().isEmpty()) {
                    HPacket awaitingPacket = gAsync.awaitPacket(new AwaitingPacket("ClickFurni", HMessage.Direction.TOSERVER, 1000));

                    if (awaitingPacket != null) {
                        int furniId = awaitingPacket.readInteger();

                        if (getLocation(point.getX(), point.getY()) == null) {
                            Location location = new Location(point.getX(), point.getY(), furniId, isSittedLocationCheckbox.isSelected());
                            locationList.add(location);
                            Platform.runLater(() -> {
                                logLocationCheckbox.setSelected(false);
                                infoLabel.setText("Added Location at " + point.getX() + ", " + point.getY() + " with furni " + furniId + " [" + (isSittedLocationCheckbox.isSelected() ? "SITTED" : "STANDED") + "]");
                                customLocationNameTextField.setText("");
                            });
                        }
                    }else {
                        Platform.runLater(() -> {
                            logLocationCheckbox.setSelected(false);
                            infoLabel.setText("I couldn't found a furni ID, please input a custom name to the Location and try again");
                            customLocationNameTextField.setText("");
                        });
                    }
                } else {
                    Location location = getLocation(point.getX(), point.getY());
                    if (location == null) {
                        Location newLocation = new Location(point.getX(), point.getY(), 0, isSittedLocationCheckbox.isSelected());
                        newLocation.setCustomName(customLocationNameTextField.getText());
                        locationList.add(newLocation);
                        Platform.runLater(() -> {
                            logLocationCheckbox.setSelected(false);
                            infoLabel.setText("Added Location at " + point.getX() + ", " + point.getY() + " with custom name " + customLocationNameTextField.getText() + " [" + (isSittedLocationCheckbox.isSelected() ? "SITTED" : "STANDED") + "]");
                            customLocationNameTextField.setText("");
                        });
                    } else {
                        location.setCustomName(customLocationNameTextField.getText());
                    }
                }
            }).start();
        }
    }

    public static void logToFile(String message) {
        logger.info(message);
    }

    private static void setupFileLogger() {
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
            FileHandler fileHandler = new FileHandler( logFolderPath + habboUserName + "-" + currentDateTime + "-log.txt", 0, 1, true);
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
        isSittedLocationCheckbox.setSelected(cache.optBoolean("isSittedLocation"));

        logChatWebhookCheckbox.setSelected(cache.optBoolean("logChatWebhook"));
        logLocationWebhookCheckbox.setSelected(cache.optBoolean("logLocationWebhook"));
        logEntersLeavesWebhookCheckbox.setSelected(cache.optBoolean("logEntersLeavesWebhook"));
        enableWebhookCheckbox.setSelected(cache.optBoolean("enableWebhook"));
        hideWhispersWebhookCheckbox.setSelected(cache.optBoolean("hideWhispersWebhook"));

        mentionLocationsWebhookCheckbox.setSelected(cache.optBoolean("mentionLocationsWebhook"));
        mentionWhispersWebhookCheckbox.setSelected(cache.optBoolean("mentionWhispersWebhook"));

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
}
