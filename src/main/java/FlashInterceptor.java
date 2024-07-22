import gearth.extensions.parsers.*;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class FlashInterceptor {
    private final RoomLogger roomLogger;

    public FlashInterceptor(RoomLogger roomLogger) {
        this.roomLogger = roomLogger;
    }

    public void onChat(HMessage hMessage) {
        if(roomLogger.disabled) {
            return;
        }
        if (roomLogger.roomLoaded && roomLogger.logChatCheckbox.isSelected()) {
            HPacket hPacket = hMessage.getPacket();
            int index = hPacket.readInteger();
            String message = hPacket.readString(StandardCharsets.UTF_8);
            int bubble = -1;
            hPacket.readInteger();
            bubble = hPacket.readInteger();
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


    void onUserRemove(HMessage hMessage) {
        if(roomLogger.disabled || !roomLogger.roomLoaded) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();

        Player player;
        int index;

        String indexString = hPacket.readString();
        index = Integer.parseInt(indexString);

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


    void onGetGuestRoomResult(HMessage hMessage) {
        HPacket hPacket = hMessage.getPacket();

        boolean entered = hPacket.readBoolean();
        roomLogger.roomId = hPacket.readInteger();
        roomLogger.roomName = hPacket.readString(StandardCharsets.UTF_8);

        if(roomLogger.habboId == -1) {
            new Thread(() -> {
                roomLogger.sendToServer(new HPacket("InfoRetrieve", HMessage.Direction.TOSERVER));
            }).start();
        }

        if(!roomLogger.logOnlyRoomListView.getItems().contains(String.valueOf(roomLogger.roomId))) {
            roomLogger.logOnlyRooms.setSelected(false);
        }

        if (!entered) {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String currentDateTime = dateFormat.format(currentDate);
            String logRoomReset = "Room " + roomLogger.roomName + " loaded at " + currentDateTime;
            if (roomLogger.webhookEnabled) {
                roomLogger.webhook.sendLog(logRoomReset, null, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());
            }
            if(!roomLogger.disabled) {
                RoomLogger.logToFile(logRoomReset);
            }
            Platform.runLater(() -> {
                roomLogger.infoLabel.setText("Locations was resetted because room was reloaded.");
                roomLogger.updateTextArea(roomLogger.consoleTextArea, logRoomReset + "\n");
            });
            roomLogger.roomLoaded = false;
            roomLogger.playerList.clear();
            roomLogger.locationList.clear();
            Platform.runLater(() -> {
                roomLogger.locationListView.getItems().clear();
            });

        } else {
            if(Cacher.get("logLocation" + roomLogger.roomId) != null) {
                JSONArray jsonArray = (JSONArray) Cacher.get("logLocation" + roomLogger.roomId);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonLoc = jsonArray.getJSONObject(i);
                    roomLogger.locationList.add(new Location(jsonLoc.getInt("x"), jsonLoc.getInt("y"), jsonLoc.getString("name")));
                    Platform.runLater(() -> {
                        roomLogger.locationListView.getItems().add("(" + jsonLoc.getInt("x") + ", " + jsonLoc.getInt("y") + ") " + jsonLoc.getString("name"));
                    });
                }

            }
            roomLogger.initialEntryOnRoom = false;
        }

        if (!roomLogger.roomLoaded && !entered) {
            Platform.runLater(() -> {
                roomLogger.logLocationCheckbox.setDisable(false);
                roomLogger.customLocationNameTextField.setDisable(false);
                roomLogger.customNameLabel.setDisable(false);
                roomLogger.infoLabel.setText("Room was loaded, extension ready, any question ask in discord > thauanvargas <");
            });
            roomLogger.roomLoaded = true;
            roomLogger.initialEntryOnRoom = true;
        }

    }


    void onExpression(HMessage hMessage) {
        if(roomLogger.disabled) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();
        int index = hPacket.readInteger();
        int expression = hPacket.readInteger();
        Player player = roomLogger.findPlayerByIndex(index);

        if (player != null && roomLogger.logUserActionsCheckbox.isSelected()) {
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
                RoomLogger.logToFile(logChat);
                Platform.runLater(() -> {
                    roomLogger.updateTextArea(roomLogger.consoleTextArea, logChat + "\n");
                });
            }
        }
    }


    void onUsers(HMessage hMessage) {
        new Thread(() -> {
            boolean isInitial = roomLogger.initialEntryOnRoom;
            if (roomLogger.roomLoaded) {
                try {
                    HPacket hPacket = hMessage.getPacket();
                    HEntity[] roomUsersList = HEntity.parse(hPacket);

                    for (HEntity hEntity : roomUsersList) {
                        if (hEntity.getName().equals(RoomLogger.habboUserName)) {
                            roomLogger.habboIndex = hEntity.getIndex();
                        }

                        if (hEntity.getEntityType() == HEntityType.PET) {
                            continue;
                        }
                        Player player;
                        player = roomLogger.findPlayerById(hEntity.getId());


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
        }).start();
    }


    void onUserUpdate(HMessage hMessage) {
        if(roomLogger.disabled) {
            return;
        }
        if (roomLogger.roomLoaded) {
            try {
                for (HEntityUpdate hEntityUpdate : HEntityUpdate.parse(hMessage.getPacket())) {
                    HStance hStance = hEntityUpdate.getStance();
                    int currentIndex = hEntityUpdate.getIndex();
                    int currentX = hEntityUpdate.getTile().getX();
                    int currentY = hEntityUpdate.getTile().getY();
                    Player player = roomLogger.findPlayerByIndex(currentIndex);
                    Location location = roomLogger.getLocation(currentX, currentY);
                    Date currentDate = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    String currentDateTime = dateFormat.format(currentDate);

                    if (player == null) {
                        continue;
                    }

                    if (roomLogger.logUserActionsCheckbox.isSelected()) {
                        if (hEntityUpdate.getSign() != null) {
                            String log = "[Action] " + player.getName() + " showed sign " + hEntityUpdate.getSign() + "  at " + currentDateTime;
                            RoomLogger.logToFile(log);
                            Platform.runLater(() -> {
                                roomLogger.updateTextArea(roomLogger.consoleTextArea, log + "\n");
                            });
                        }
                    }

                    if (location == null) {
                        continue;
                    }else {
                        if(hEntityUpdate.getAction() == HAction.None || hEntityUpdate.getAction() == HAction.Sit) {
                            player.setCoordX(currentX);
                            player.setCoordY(currentY);

                            String log = RoomLogger.getLocationLog(player, location, currentDateTime, false, hEntityUpdate.getAction() == HAction.Sit);
                            RoomLogger.logToFile(log);
                            if (roomLogger.webhookEnabled && roomLogger.logLocationWebhookCheckbox.isSelected() && !roomLogger.disabled) {
                                roomLogger.webhook.sendLog(log, player, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());
                            }
                            Platform.runLater(() -> {
                                roomLogger.updateTextArea(roomLogger.consoleTextArea, log + "\n");
                                roomLogger.updateTextArea(roomLogger.consoleLogLocations, log + "\n");
                            });
                        }
                    }

                    if (hEntityUpdate.getAction() == HAction.Move) {
                        if (player.getLocation() != null) {
                            String log = RoomLogger.getLocationLog(player, player.getLocation(), currentDateTime, true, hEntityUpdate.getAction() == HAction.Sit);
                            RoomLogger.logToFile(log);
                            if (roomLogger.webhookEnabled && roomLogger.logLocationWebhookCheckbox.isSelected() && !roomLogger.disabled) {
                                roomLogger.webhook.sendLog(log, player, roomLogger.mentionWhispersWebhookCheckbox.isSelected(), roomLogger.mentionLocationsWebhookCheckbox.isSelected());
                            }
                            Platform.runLater(() -> {
                                roomLogger.updateTextArea(roomLogger.consoleTextArea, log + "\n");
                                roomLogger.updateTextArea(roomLogger.consoleLogLocations, log + "\n");
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



    void onDance(HMessage hMessage) {
        if(roomLogger.disabled) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();
        int index = hPacket.readInteger();
        int dance = hPacket.readInteger();
        Player player = roomLogger.findPlayerByIndex(index);

        if (player != null && roomLogger.logUserActionsCheckbox.isSelected()) {
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
            RoomLogger.logToFile(logChat);
            Platform.runLater(() -> {
                roomLogger.updateTextArea(roomLogger.consoleTextArea, logChat + "\n");
            });
        }
    }


    protected void onSetupLocation(HMessage hMessage) {
        if(roomLogger.disabled) {
            return;
        }
        HPacket hPacket = hMessage.getPacket();
        HPoint point = new HPoint(hPacket.readInteger(), hPacket.readInteger());

        if (roomLogger.logLocationCheckbox.isSelected()) {
            new Thread(() -> {
                Location location = roomLogger.getLocation(point.getX(), point.getY());
                if (location == null) {
                    Location newLocation = new Location(point.getX(), point.getY(), roomLogger.customLocationNameTextField.getText());
                    roomLogger.locationList.add(newLocation);
                    Platform.runLater(() -> {
                        roomLogger.logLocationCheckbox.setSelected(false);
                        if(roomLogger.customLocationNameTextField.getText().isEmpty()) {
                            roomLogger.infoLabel.setText("Added Location at (" + point.getX() + ", " + point.getY() + ")");
                        } else {
                            roomLogger.infoLabel.setText("Added Location at (" + point.getX() + ", " + point.getY() + ") with name " + roomLogger.customLocationNameTextField.getText());
                        }
                        roomLogger.locationListView.getItems().add("(" + point.getX() + ", " + point.getY() + ")" + " " + roomLogger.customLocationNameTextField.getText());
                        roomLogger.customLocationNameTextField.setText("");

                        roomLogger.saveLocationsToCache();
                    });

                } else {
                    location.setCustomName(roomLogger.customLocationNameTextField.getText());
                }
            }).start();
        }
    }

}
