import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import javafx.application.Platform;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class Webhook {

    private String webHookUrl = "";
    private WebhookClient client;
    private String discordUsernames = "";

    public void setupWebhook() {
        if(!webHookUrl.isEmpty()) {
            WebhookClientBuilder builder = new WebhookClientBuilder(webHookUrl);
            builder.setThreadFactory((job) -> {
                Thread thread = new Thread(job);
                thread.setName("Room Logger");
                thread.setDaemon(true);
                return thread;
            });
            builder.setWait(true);

            WebhookClient.setDefaultErrorHandler((client, message, throwable) -> {
                System.err.printf("[%s] %s%n", client.getId(), message);
                if (throwable != null)
                    throwable.printStackTrace();

                if (throwable instanceof Exception) {
                    client.close();
                }
            });

            client = builder.build();
        }else {
            System.out.println("Webhook is empty!");
        }
    }

    public void sendLog(String log, Player player, boolean isMentionWhisper, boolean isMentionLocation) {
        try {

        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        if(player == null) {
            builder.setUsername("Room Logger by Thauan");
            builder.setAvatarUrl("https://i.imgur.com/Lbikobh.png");
            builder.setContent("*" + log + "*");
        }

        if(player != null) {
            builder.setUsername(player.getName());
            builder.setAvatarUrl(RoomLogger.isOrigins ?
                    "https://pbs.twimg.com/profile_images/1801173556841050112/zJJ4JZKU_400x400.jpg" :
                    "https://www.habbo.com/habbo-imaging/avatarimage?direction=2&head_direction=3&action=wav&gesture=sml&size=m&figure=" + player.getFigureId());
            if (log.startsWith("[Shout]")) {
                builder.setContent("*" + log + "*");
            } else {
                builder.setContent("*" + log + "*");
            }


            if (log.startsWith("[Join]")) {
                builder.setUsername("Room Logger by Thauan (#" + player.getIndex() + ")");
                WebhookEmbed embed = new WebhookEmbedBuilder()
                        .setColor(0x4CAF50)
                        .setDescription(player.getName() + " entered the room")
                        .build();
                builder.setAvatarUrl(RoomLogger.isOrigins ?
                        "https://pbs.twimg.com/profile_images/1801173556841050112/zJJ4JZKU_400x400.jpg" :
                        "https://www.habbo.com/habbo-imaging/avatarimage?direction=2&head_direction=3&action=wav&gesture=sml&size=m&figure=" + player.getFigureId());
                builder.setContent("");
                builder.addEmbeds(embed);
                client.send(builder.build());
                return;
            }

            if (log.startsWith("[Leave]")) {
                builder.setUsername("Room Logger by Thauan (#" + player.getIndex() + ")");
                WebhookEmbed embed = new WebhookEmbedBuilder()
                        .setColor(0xF80000)
                        .setDescription(player.getName() + " has left the room")
                        .build();
                builder.setAvatarUrl(RoomLogger.isOrigins ?
                        "https://pbs.twimg.com/profile_images/1801173556841050112/zJJ4JZKU_400x400.jpg" :
                        "https://www.habbo.com/habbo-imaging/avatarimage?direction=2&head_direction=3&action=wav&gesture=sml&size=m&figure=" + player.getFigureId());
                builder.setContent("");
                builder.addEmbeds(embed);
                client.send(builder.build());
                return;
            }

            if(log.startsWith("[Whisper]") && isMentionWhisper) {
                builder.setContent("||" + discordUsernames + "||" + log);
            }

            if(log.startsWith("[Location]") && isMentionLocation) {
                builder.setContent("||" + discordUsernames + "||" + log);
            }
        }


        client.send(builder.build());

        } catch (Exception e) {
            RoomLogger.logToFile("Webhook Crashed, we will attempt to restart it.");
            setupWebhook();
        }

    }

    public boolean testWebhook() {
        boolean isOK = false;
        try {
            client.send("***** RoomLogger by Thauan Test Message *****");
            isOK = true;
        }catch (Exception e) {}
        return isOK;
    }

    public void setWebHookUrl(String webHookUrl) {
        this.webHookUrl = webHookUrl;
    }

    public void setDiscordUsernames(String discordUsernames) {
        this.discordUsernames = Arrays.stream(discordUsernames.split(","))
                .map(String::trim)
                .map(userId -> {
                    String formattedUsername = userId.startsWith("@") ? userId : "@" + userId;
                    return "<" + formattedUsername.toUpperCase() + ">";
                })
                .collect(Collectors.joining(" "));
    }

    public WebhookClient getClient() {
        return client;
    }
}
