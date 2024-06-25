import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.GAsync;
import gearth.extensions.parsers.*;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import parsers.OHEntity;

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
import java.util.stream.IntStream;

@ExtensionInfo(
        Title = "Room Logger",
        Description = "The Best Habbo Chat Logger",
        Version = "1.3",
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
    private final Webhook webhook = new Webhook();
    public Label usernameLabel;
    public ListView<String> locationListView;
    public TextArea consoleLogLocations;
    public Button clearAllLogLocations;
    public Button removeLogLocation;
    public Label yourUserNameLabel;
    public Tab locationsTab;
    public Label clientVersionLabel;

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
                clientVersionLabel.setText("FLASH");
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

        intercept(HMessage.Direction.TOCLIENT, "CHAT", this::onChat);

        intercept(HMessage.Direction.TOCLIENT, "CHAT_2", this::onChat);

        intercept(HMessage.Direction.TOCLIENT, "CHAT_3", this::onChat);

        intercept(HMessage.Direction.TOCLIENT, "USERS", this::onUsersOrigin);

        intercept(HMessage.Direction.TOCLIENT, "FLATINFO", this::onFlatInfo);

        intercept(HMessage.Direction.TOCLIENT, "ROOM_READY", this::onRoomReady);

        intercept(HMessage.Direction.TOCLIENT, "LOGOUT", this::onUserRemove);

        intercept(HMessage.Direction.TOCLIENT, "STATUS", this::onStatus);

        intercept(HMessage.Direction.TOCLIENT, "USER_OBJ", this::onUserObject);


    }

    private void onUserObject(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        final byte[] dataRemainder = hPacket.readBytes(hPacket.length() - hPacket.getReadIndex());
        final String data = new String(dataRemainder, StandardCharsets.ISO_8859_1);

        String[] pairs = data.split("\r");

        String nameValue = null;

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("name")) {
                nameValue = keyValue[1];
                break;
            }
        }

        habboUserName = nameValue;

        if(habboUserName != null && !loggerSetuped) {
            Platform.runLater(() -> {
                usernameLabel.setText(habboUserName);
            });

            System.setProperty("java.util.logging.SimpleFormatter.format", "RoomLogger: %5$s%n");
            RoomLogger.setupFileLogger();
            loggerSetuped = true;

            new Thread(() -> {
                JSONObject cache = Cacher.getCacheContents();

                disableLogWithHabboCheckbox.setSelected(cache.optBoolean("disabledLog" + habboUserName));

                if(disableLogWithHabboCheckbox.isSelected()) {
                    disabled = true;
                }
            }).start();


        }

        Platform.runLater(() -> {
            usernameLabel.setText(habboUserName);
        });
    }

    private void onStatus(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();

        hPacket.readInteger();
        int index = hPacket.readInteger();
        int x = hPacket.readInteger();
        int y = hPacket.readInteger();
        String z = hPacket.readString();
    }

    private void onRoomReady(HMessage hMessage) {
        if (!roomLoaded) {
            Platform.runLater(() -> {
                logLocationCheckbox.setDisable(false);
                customLocationNameTextField.setDisable(false);
                customNameLabel.setDisable(false);
                infoLabel.setText("Room was loaded, extension ready, any question ask in discord > thauanvargas <");
            });
            roomLoaded = true;
            initialEntryOnRoom = true;
        }
        playerList.clear();
        locationList.clear();
    }

    private void onFlatInfo(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        boolean entered = hPacket.readBoolean();
        hPacket.readString();
        hPacket.readString();
        roomName = hPacket.readString();
        System.out.println(roomName);

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String currentDateTime = dateFormat.format(currentDate);
        String logRoomReset = "Room " + roomName + " loaded at " + currentDateTime;
        if (webhookEnabled) {
            webhook.sendLog(logRoomReset, null, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
        }
        if(!disabled) {
            logToFile(logRoomReset);
        }
        Platform.runLater(() -> {
            infoLabel.setText("Room Resetted.");
            consoleTextArea.appendText(logRoomReset + "\n");
        });

        roomLoaded = false;
    }

    private void onGetGuestRoomResult(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();

        boolean entered = hPacket.readBoolean();
        System.out.println(entered);
        roomId = hPacket.readInteger();
        System.out.println(roomId);
        roomName = hPacket.readString(StandardCharsets.UTF_8);
        System.out.println(roomName);

        if(habboId == -1) {
            new Thread(() -> {
                sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
            }).start();
        }

        if (!entered) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentDateTime = dateFormat.format(currentDate);
            String logRoomReset = "Room " + roomName + " loaded at " + currentDateTime;
            if (webhookEnabled) {
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
            Platform.runLater(() -> {
                locationListView.getItems().clear();
            });

        } else {
            if(Cacher.get("logLocation" + roomId) != null) {
                JSONArray jsonArray = (JSONArray) Cacher.get("logLocation" + roomId);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonLoc = jsonArray.getJSONObject(i);
                    locationList.add(new Location(jsonLoc.getInt("x"), jsonLoc.getInt("y"), jsonLoc.getString("name")));
                    Platform.runLater(() -> {
                        locationListView.getItems().add("(" + jsonLoc.getInt("x") + ", " + jsonLoc.getInt("y") + ") " + jsonLoc.getString("name"));
                    });
                }

                System.out.println(Arrays.toString(locationList.toArray()));
            }
            initialEntryOnRoom = false;
        }

        if (!roomLoaded && !entered) {
            Platform.runLater(() -> {
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
        if(disabled) {
            return;
        }
        if (roomLoaded && logChatCheckbox.isSelected()) {
            HPacket hPacket = hMessage.getPacket();
            int index = hPacket.readInteger();
            String message = hPacket.readString(isOrigins ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8);
            int bubble = -1;
            if(!isOrigins) {
                hPacket.readInteger();
                bubble = hPacket.readInteger();
            }
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

                if (Objects.equals(hash, "CHAT_2")) {
                    hash = "Whisper";
                }

                if (Objects.equals(hash, "CHAT_3")) {
                    hash = "Shout";
                }

                if (Objects.equals(hash, "CHAT")) {
                    hash = "Chat";
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
        if(disabled) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();
        int index = hPacket.readInteger();
        int expression = hPacket.readInteger();
        Player player = findPlayerByIndex(index);

        if (player != null && logUserActionsCheckbox.isSelected()) {
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
        if(disabled) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();
        int index = hPacket.readInteger();
        int dance = hPacket.readInteger();
        Player player = findPlayerByIndex(index);

        if (player != null && logUserActionsCheckbox.isSelected()) {
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


    private void onUsersOrigin(HMessage hMessage) {
        new Thread(() -> {
            boolean isInitial = initialEntryOnRoom;
            if (roomLoaded) {
                try {
                    HPacket hPacket = hMessage.getPacket();
                    OHEntity[] roomUsersList = OHEntity.parse(hPacket);

                    for (OHEntity hEntity : roomUsersList) {
                        if (hEntity.getName().equals(habboUserName)) {
                            habboIndex = hEntity.getIndex();
                        }

                        if (hEntity.getEntityType() == HEntityType.PET) {
                            continue;
                        }
                        Player player;
                        if(!isOrigins)
                            player = findPlayerById(hEntity.getId());
                        else
                            player = findPlayerByUserName(hEntity.getName());


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
            if(isOrigins && isInitial) {
                initialEntryOnRoom = false;
            }
        }).start();
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
                        Player player;
                        if(!isOrigins)
                            player = findPlayerById(hEntity.getId());
                        else
                            player = findPlayerByUserName(hEntity.getName());


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
            if(isOrigins && isInitial) {
                initialEntryOnRoom = false;
            }
        }).start();
    }

    private void onUserUpdate(HMessage hMessage) {
        if(disabled) {
            return;
        }
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
                    }else {
                        if(hEntityUpdate.getAction() == HAction.None || hEntityUpdate.getAction() == HAction.Sit) {
                            player.setCoordX(currentX);
                            player.setCoordY(currentY);

                            String log = getLocationLog(player, location, currentDateTime, false, hEntityUpdate.getAction() == HAction.Sit);
                            logToFile(log);
                            if (webhookEnabled && logLocationWebhookCheckbox.isSelected() && !disabled) {
                                webhook.sendLog(log, player, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
                            }
                            Platform.runLater(() -> {
                                consoleTextArea.appendText(log + "\n");
                                consoleLogLocations.appendText(log + "\n");
                            });
                        }
                    }

                    if (hEntityUpdate.getAction() == HAction.Move) {
                        if (player.getLocation() != null) {
                            String log = getLocationLog(player, player.getLocation(), currentDateTime, true, hEntityUpdate.getAction() == HAction.Sit);
                            logToFile(log);
                            if (webhookEnabled && logLocationWebhookCheckbox.isSelected() && !disabled) {
                                webhook.sendLog(log, player, mentionWhispersWebhookCheckbox.isSelected(), mentionLocationsWebhookCheckbox.isSelected());
                            }
                            Platform.runLater(() -> {
                                consoleTextArea.appendText(log + "\n");
                                consoleLogLocations.appendText(log + "\n");
                            });
                            player.setLocation(null);
                            continue;
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
        if(disabled || !roomLoaded) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();

        Player player;
        int index;

        if(!isOrigins) {
            String indexString = hPacket.readString();
            index = Integer.parseInt(indexString);
        }else {
            final byte[] dataRemainder = hPacket.readBytes(hPacket.getBytesLength() - hPacket.getReadIndex());
            final String data = new String(dataRemainder, StandardCharsets.ISO_8859_1);
            index = Integer.parseInt(data);
        }

        player = findPlayerByIndex(index);

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

    private static String getLocationLog(Player player, Location location, String currentDateTime, boolean left, boolean sitted) {
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

    protected void onSetupLocation(HMessage hMessage) {
        if(disabled) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();
        HPoint point = new HPoint(hPacket.readInteger(), hPacket.readInteger());

        if (logLocationCheckbox.isSelected()) {
            new Thread(() -> {
                Location location = getLocation(point.getX(), point.getY());
                if (location == null) {
                    Location newLocation = new Location(point.getX(), point.getY(), customLocationNameTextField.getText());
                    locationList.add(newLocation);
                    Platform.runLater(() -> {
                        logLocationCheckbox.setSelected(false);
                        if(customLocationNameTextField.getText().isEmpty()) {
                            infoLabel.setText("Added Location at (" + point.getX() + ", " + point.getY() + ")");
                        } else {
                            infoLabel.setText("Added Location at (" + point.getX() + ", " + point.getY() + ") with name " + customLocationNameTextField.getText());
                        }
                        locationListView.getItems().add("(" + point.getX() + ", " + point.getY() + ")" + " " + customLocationNameTextField.getText());
                        customLocationNameTextField.setText("");

                        saveLocationsToCache();
                    });

                } else {
                    location.setCustomName(customLocationNameTextField.getText());
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

    private void saveLocationsToCache() {
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
}
