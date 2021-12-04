package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class AttachmentHandler extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.size() == 0) return;
        User author = message.getAuthor();
        MessageChannel channel = message.getChannel();
        List<String> extensions = List.of("txt", "log", "gz");
        for (var attachment : attachments) {
            if (!extensions.contains(attachment.getFileExtension())) {
                continue;
            }
            attachment.retrieveInputStream()
                .thenAccept(stream -> {
                    StringBuilder builder = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int count;
                    try (InputStream logStream = attachment.getFileExtension().equals("gz") ? new GZIPInputStream(stream) : stream) {
                        while ((count = logStream.read(buffer)) > 0) {
                            builder.append(new String(buffer, 0, count));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    final String tokenKey = "--accessToken";
                    int indexOfToken = builder.indexOf(tokenKey);
                    if (indexOfToken != -1) {
                        int endOfToken = builder.indexOf(" ", indexOfToken + tokenKey.length() + 1);
                        builder.replace(indexOfToken + tokenKey.length() + 1, endOfToken, "**removed acess token**");
                    }
                    try {
                        channel.sendMessageEmbeds(linkEmbed(attachment.getFileName(), author)).setActionRow(
                                Button.link(post(builder.toString()), "Click here to view")
                                ).queue();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            });
        }
    }

    private MessageEmbed linkEmbed(String fileName, User user) {
        return new EmbedBuilder()
                .setTitle("`" + fileName + "` uploaded to Hastebin")
                .setColor(0x9992ff)
                .setFooter("Uploaded by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .build();
    }

    private String post(String text) throws IOException {
        byte[] postData = text.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        String requestURL = "https://www.toptal.com/developers/hastebin/documents";
        URL url = new URL(requestURL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Hastebin Java Api");
        connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        connection.setUseCaches(false);
        String response = null;
        try (DataOutputStream stream = new DataOutputStream(connection.getOutputStream())) {
            stream.write(postData);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            response = reader.readLine();
        }
        if (response.contains("\"key\"")) {
            response = response.substring(response.indexOf(":") + 2, response.length() - 2);
            String postURL = "https://www.toptal.com/developers/hastebin/";
            response = postURL + response;
        }
        return response;
    }

}
