import entities.OHItem;
import gearth.extensions.parsers.HEntityType;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class OriginsInterceptor {
    private final RoomLogger roomLogger;

    public OriginsInterceptor(RoomLogger roomLogger) {
        this.roomLogger = roomLogger;
    }

    public void onChat(HMessage hMessage) {
        if(roomLogger.disabled) {
            return;
        }
        if (roomLogger.roomLoaded && roomLogger.logChatCheckbox.isSelected()) {
            HPacket hPacket = hMessage.getPacket();
            int index = hPacket.readInteger();
            String message = hPacket.readString(StandardCharsets.ISO_8859_1);
            Player player = roomLogger.findPlayerByIndex(index);

            if (player != null) {
                boolean isBot = player.isBot();
                if (player.isBot() && !roomLogger.logChatBotsCheckbox.isSelected()) {
                    return;
                }
                Date currentDate = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                String currentDateTime = dateFormat.format(currentDate);
                String hash = roomLogger.getPacketInfoManager().getPacketInfoFromHeaderId(HMessage.Direction.TOCLIENT, hPacket.headerId()).getName();

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
                RoomLogger.logToFile(logChat);
                Platform.runLater(() -> {
                    roomLogger.updateTextArea(roomLogger.consoleTextArea, logChat + "\n");
                });
                if (roomLogger.webhookEnabled && roomLogger.logChatWebhookCheckbox.isSelected() && !isBot &&
                        !Objects.equals(hash, "Wired") && !roomLogger.disabled) {

                    boolean isWhisper = Objects.equals(hash, "Whisper");

                    if (isWhisper && roomLogger.hideWhispersWebhookCheckbox.isSelected()) {
                        return;
                    }

                    roomLogger.webhook.sendLog(logChat, player, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());

                }
            }
        }
    }


    void onFlatInfo(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        boolean entered = hPacket.readBoolean();
        roomLogger.roomOwner = hPacket.readString().substring(4);
        System.out.println(roomLogger.roomOwner);
        System.out.println(hPacket.readString());
        roomLogger.roomName = hPacket.readString();
        System.out.println(roomLogger.roomName);

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String currentDateTime = dateFormat.format(currentDate);
        if(!Objects.equals(roomLogger.roomOwner, "-")) {
            String logRoomReset = "Room " + roomLogger.roomName + " of user " + roomLogger.roomOwner + " loaded at " + currentDateTime;
            if (roomLogger.webhookEnabled) {
                roomLogger.webhook.sendLog(logRoomReset, null, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());
            }
            if(!roomLogger.disabled) {
                RoomLogger.logToFile(logRoomReset);
            }
            Platform.runLater(() -> {
                roomLogger.infoLabel.setText("Room Resetted.");
                roomLogger.updateTextArea(roomLogger.consoleTextArea, logRoomReset + "\n");
            });
        }

        roomLogger.roomLoaded = false;
    }

    void onRoomReady(HMessage hMessage) {
        if (!roomLogger.roomLoaded) {
            Platform.runLater(() -> {
                roomLogger.logLocationCheckbox.setDisable(false);
                roomLogger.customLocationNameTextField.setDisable(false);
                roomLogger.customNameLabel.setDisable(false);
                roomLogger.infoLabel.setText("Room was loaded, extension ready, any question ask in discord > thauanvargas <");
            });
            roomLogger.roomLoaded = true;
            roomLogger.initialEntryOnRoom = true;
        }
        roomLogger.playerList.clear();
        roomLogger.locationList.clear();
    }


    void onUserObject(HMessage hMessage) {
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

        RoomLogger.habboUserName = nameValue;

        if(RoomLogger.habboUserName != null && !roomLogger.loggerSetuped) {
            Platform.runLater(() -> {
                roomLogger.usernameLabel.setText(RoomLogger.habboUserName);
            });

            System.setProperty("java.util.logging.SimpleFormatter.format", "RoomLogger: %5$s%n");
            RoomLogger.setupFileLogger();
            roomLogger.loggerSetuped = true;

            new Thread(() -> {
                JSONObject cache = Cacher.getCacheContents();

                roomLogger.disableLogWithHabboCheckbox.setSelected(cache.optBoolean("disabledLog" + RoomLogger.habboUserName));

                if(roomLogger.disableLogWithHabboCheckbox.isSelected()) {
                    roomLogger.disabled = true;
                }
            }).start();


        }

        Platform.runLater(() -> {
            roomLogger.usernameLabel.setText(RoomLogger.habboUserName);
        });
    }


    void onStatus(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();

        hPacket.readInteger();
        int index = hPacket.readInteger();
        int x = hPacket.readInteger();
        int y = hPacket.readInteger();
        String z = hPacket.readString();
    }


    void onUserRemove(HMessage hMessage) {
        if(roomLogger.disabled || !roomLogger.roomLoaded) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();

        Player player;
        int index;

        final byte[] dataRemainder = hPacket.readBytes(hPacket.getBytesLength() - hPacket.getReadIndex());
        final String data = new String(dataRemainder, StandardCharsets.ISO_8859_1);
        index = Integer.parseInt(data);


        player = roomLogger.findPlayerByIndex(index);

        if (player != null) {
            if (roomLogger.logEntersLeavesCheckbox.isSelected()) {
                Date currentDate = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String currentDateTime = dateFormat.format(currentDate);
                String logLeave = "[Leave] Player > " + player.getName() + " < left the room at " + currentDateTime;
                RoomLogger.logToFile(logLeave);
                if (roomLogger.webhookEnabled && roomLogger.logEntersLeavesWebhookCheckbox.isSelected() && !roomLogger.disabled) {
                    roomLogger.webhook.sendLog(logLeave, player, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());
                }
                Platform.runLater(() -> {
                    roomLogger.updateTextArea(roomLogger.consoleTextArea, logLeave + "\n");
                });
            }
            roomLogger.playerList.remove(player);
        }

    }


    void onUsersOrigin(HMessage hMessage) {
        new Thread(() -> {
            boolean isInitial = roomLogger.initialEntryOnRoom;
            if (roomLogger.roomLoaded) {
                try {
                    HPacket hPacket = hMessage.getPacket();
                    parsers.OHEntity[] roomUsersList = parsers.OHEntity.parse(hPacket);

                    for (parsers.OHEntity hEntity : roomUsersList) {
                        if (hEntity.getName().equals(roomLogger.habboUserName)) {
                            roomLogger.habboIndex = hEntity.getIndex();
                        }

                        if (hEntity.getEntityType() == HEntityType.PET) {
                            continue;
                        }
                        Player player;
                        player = roomLogger.findPlayerByUserName(hEntity.getName());


                        if (player == null) {
                            player = new Player(hEntity.getId(), hEntity.getIndex(), hEntity.getName());
                            if (hEntity.getEntityType() == HEntityType.BOT || hEntity.getEntityType() == HEntityType.OLD_BOT) {
                                player.setBot(true);
                            } else {
                                player.setFigureId(hEntity.getFigureId());
                            }
                            player.setCoordX(-1);
                            player.setCoordY(-1);
                            roomLogger.playerList.add(player);
                            Date currentDate = new Date();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            String currentDateTime = dateFormat.format(currentDate);
                            String logEntered = "";
                            if (!isInitial) {
                                logEntered = "[Join] Player > " + player.getName() + " < entered the room at " + currentDateTime;
                            } else {
                                logEntered = "Player > " + player.getName() + " < is at the room on load ";
                            }
                            if (roomLogger.logEntersLeavesCheckbox.isSelected() && !roomLogger.disabled) {
                                RoomLogger.logToFile(logEntered);
                                String finalLogEntered = logEntered;
                                Platform.runLater(() -> {
                                    roomLogger.updateTextArea(roomLogger.consoleTextArea, finalLogEntered + "\n");
                                });
                            }
                            if (roomLogger.webhookEnabled && roomLogger.logEntersLeavesWebhookCheckbox.isSelected() && !isInitial && !roomLogger.disabled) {
                                roomLogger.webhook.sendLog(logEntered, player, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());
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
            if(isInitial) {
                roomLogger.initialEntryOnRoom = false;
            }
        }).start();
    }

    void onItems(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();
        new Thread(() -> {
            if (Objects.equals(roomLogger.roomOwner, "-")) {
                Date currentDate = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String currentDateTime = dateFormat.format(currentDate);
                OHItem[] items = OHItem.parse(hPacket);
                roomLogger.roomOwner = items[0].getOwner();
                String logRoomReset = "Room " + roomLogger.roomName + " of user " + roomLogger.roomOwner + " loaded at " + currentDateTime;
                if (roomLogger.webhookEnabled) {
                    roomLogger.webhook.sendLog(logRoomReset, null, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());
                }
                if(!roomLogger.disabled) {
                    RoomLogger.logToFile(logRoomReset);
                }
                Platform.runLater(() -> {
                    roomLogger.infoLabel.setText("Room Resetted.");
                    roomLogger.updateTextArea(roomLogger.consoleTextArea, logRoomReset + "\n");
                });
            }
        }).start();
    }
}
