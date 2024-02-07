import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.tools.AwaitingPacket;
import gearth.extensions.extra.tools.GAsync;
import gearth.extensions.parsers.*;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@ExtensionInfo(
        Title = "Room Logger",
        Description = "Made with Luv",
        Version = "1.0",
        Author = "Thauan"
)

public class RoomLogger extends ExtensionForm implements Initializable {
    private static final Logger logger = Logger.getLogger(RoomLogger.class.getName());

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
        RoomLogger.setupFileLogger();
    }

    public static Stage primaryStage;
    public CheckBox logLocationCheckbox;
    public CheckBox isSittedLocationCheckbox;
    public TextField customLocationNameTextField;
    public TextArea consoleTextArea;
    public Label infoLabel;
    public Label customNameLabel;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    protected String host;
    protected int habboId;
    protected String habboUserName;
    protected int habboIndex = -1;
    protected List<Player> playerList = new ArrayList<>();
    protected List<Location> locationList = new ArrayList<>();
    protected GAsync gAsync;
    public boolean roomLoaded = false;

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
        });

        intercept(HMessage.Direction.TOSERVER, "GetGuestRoom", hMessage -> {
            roomLoaded = true;
            Platform.runLater(() -> {
                isSittedLocationCheckbox.setDisable(false);
                logLocationCheckbox.setDisable(false);
                customLocationNameTextField.setDisable(false);
                customNameLabel.setDisable(false);
                infoLabel.setText("Room was loaded, extension ready, any question ask in discord > thauanvargas <");
            });
        });

        intercept(HMessage.Direction.TOCLIENT, "Users",this::onUsers);

        intercept(HMessage.Direction.TOCLIENT, "UserUpdate", this::onUserUpdate);

        intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::onSetupLocation);

        intercept(HMessage.Direction.TOCLIENT, "UserRemove", this::onUserRemove);

    }

    private void onUsers(HMessage hMessage) {
        if (roomLoaded) {
            try {
                HPacket hPacket = hMessage.getPacket();
                HEntity[] roomUsersList = HEntity.parse(hPacket);
                for (HEntity hEntity : roomUsersList) {
                    if (hEntity.getName().equals(habboUserName)) {
                        habboIndex = hEntity.getIndex();
                    }

                    Player player = findPlayerById(hEntity.getId());

                    if (player == null) {
                        player = new Player(hEntity.getId(), hEntity.getIndex(), hEntity.getName());
                        playerList.add(player);
                        Date currentDate = new Date();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        String currentDateTime = dateFormat.format(currentDate);
                        String logEntered = "Player > " + player.getName() + " < entered the room at " + currentDateTime;
                        System.out.println(logEntered);
                        logToFile(logEntered);
                        Platform.runLater(() -> {
                            consoleTextArea.appendText(logEntered + "\n");
                        });
                    } else {
                        player.setIndex(hEntity.getIndex());
                        player.setCoordX(-1);
                        player.setCoordY(-1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

                    if (location != null) {
                        if ((location.isSitted() && hStance == HStance.Sit) || !location.isSitted()) {
                            String log = getLog(player, location, currentDateTime, false);
                            System.out.println(log);
                            logToFile(log);
                            Platform.runLater(() -> {
                                consoleTextArea.appendText(log + "\n");
                            });
                        }
                    } else {
                        if (player.getLocation() != null) {
                            String log = getLog(player, player.getLocation(), currentDateTime, true);
                            System.out.println(log);
                            logToFile(log);
                            Platform.runLater(() -> {
                                consoleTextArea.appendText(log + "\n");
                            });
                            player.setLocation(null);
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
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentDateTime = dateFormat.format(currentDate);
            String logLeave = "Player > " + player.getName() + " < left the room at " + currentDateTime;
            System.out.println(logLeave);
            logToFile(logLeave);
            Platform.runLater(() -> {
                consoleTextArea.appendText(logLeave + "\n");
            });
            playerList.remove(player);
        }

    }

    private static String getLog(Player player, Location location, String currentDateTime, boolean left) {
        String log = "Player > " + player.getName() + " < ";

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
            FileHandler fileHandler = new FileHandler(logFolderPath + currentDateTime + "-log.txt", 0, 1, true);
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);

            logger.addHandler(fileHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
