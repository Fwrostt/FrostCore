package dev.frost.frostcore.utils;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class DW {
    private final String url;
    private String content;
    private String username;
    private String avatarUrl;
    private boolean tts;
    private File file;
    private List<EmbedObject> embeds = new ArrayList<EmbedObject>();

    public DW(String url) {
        this.url = url;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setTts(boolean tts) {
        this.tts = tts;
    }

    public void addEmbed(EmbedObject embed) {
        this.embeds.add(embed);
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void execute() throws IOException {
        if (this.content == null && this.embeds.isEmpty() && this.file == null) {
            throw new IllegalArgumentException("Set content, add at least one EmbedObject, or provide a file");
        }
        String boundary = "------WebKitFormBoundary" + System.currentTimeMillis();
        URL url = new URL(this.url);
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_");
        connection.setDoOutput(true);
        OutputStream outputStream = connection.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);
        if (this.content != null || !this.embeds.isEmpty()) {
            JSONObject json = new JSONObject();
            json.put("content", this.content);
            json.put("username", this.username);
            json.put("avatar_url", this.avatarUrl);
            json.put("tts", this.tts);
            if (!this.embeds.isEmpty()) {
                ArrayList<JSONObject> embedObjects = new ArrayList<JSONObject>();
                for (EmbedObject embed : this.embeds) {
                    JSONObject jsonEmbed = new JSONObject();
                    jsonEmbed.put("title", embed.getTitle());
                    jsonEmbed.put("description", embed.getDescription());
                    jsonEmbed.put("url", embed.getUrl());
                    if (embed.getColor() != null) {
                        Color color = embed.getColor();
                        int rgb = color.getRed();
                        rgb = (rgb << 8) + color.getGreen();
                        rgb = (rgb << 8) + color.getBlue();
                        jsonEmbed.put("color", rgb);
                    }

                    // Footer
                    if (embed.getFooter() != null) {
                        JSONObject footerObj = new JSONObject();
                        footerObj.put("text", embed.getFooter().getText());
                        footerObj.put("icon_url", embed.getFooter().getIconUrl());
                        jsonEmbed.put("footer", footerObj);
                    }

                    // Author
                    if (embed.getAuthor() != null) {
                        JSONObject authorObj = new JSONObject();
                        authorObj.put("name", embed.getAuthor().getName());
                        authorObj.put("url", embed.getAuthor().getUrl());
                        authorObj.put("icon_url", embed.getAuthor().getIconUrl());
                        jsonEmbed.put("author", authorObj);
                    }

                    // Thumbnail
                    if (embed.getThumbnail() != null) {
                        JSONObject thumbObj = new JSONObject();
                        thumbObj.put("url", embed.getThumbnail().getUrl());
                        jsonEmbed.put("thumbnail", thumbObj);
                    }

                    // Image
                    if (embed.getImage() != null) {
                        JSONObject imageObj = new JSONObject();
                        imageObj.put("url", embed.getImage().getUrl());
                        jsonEmbed.put("image", imageObj);
                    }

                    // Fields
                    if (!embed.getFields().isEmpty()) {
                        ArrayList<JSONObject> fieldArray = new ArrayList<>();
                        for (EmbedObject.Field field : embed.getFields()) {
                            JSONObject fieldObj = new JSONObject();
                            fieldObj.put("name", field.getName());
                            fieldObj.put("value", field.getValue());
                            fieldObj.put("inline", field.isInline());
                            fieldArray.add(fieldObj);
                        }
                        jsonEmbed.put("fields", fieldArray.toArray());
                    }

                    embedObjects.add(jsonEmbed);
                }
                json.put("embeds", embedObjects.toArray());
            }
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"payload_json\"\r\n");
            writer.append("Content-Type: application/json; charset=UTF-8\r\n\r\n");
            writer.append(json.toString()).append("\r\n");
            writer.flush();
        }
        if (this.file != null) {
            int bytesRead;
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(this.file.getName()).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();
            FileInputStream fileInputStream = new FileInputStream(this.file);
            byte[] buffer = new byte[1024];
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            fileInputStream.close();
            writer.append("\r\n");
            writer.flush();
        }
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.flush();
        writer.close();
        int responseCode = connection.getResponseCode();
        // Discord API can sometimes return 204 No Content for success
        if (responseCode < 200 || responseCode >= 300) {
            String line;
            InputStream errorStream = connection.getErrorStream();
            if (errorStream == null) {
                throw new IOException("Failed to send webhook: HTTP " + responseCode + " - No error stream returned.");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            throw new IOException("Failed to send webhook: HTTP " + responseCode + " - " + response.toString());
        }
        connection.getInputStream().close();
        connection.disconnect();
    }

    private static class JSONObject {
        private final HashMap<String, Object> map = new HashMap();

        private JSONObject() {
        }

        void put(String key, Object value) {
            if (value != null) {
                this.map.put(key, value);
            }
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            Set<Map.Entry<String, Object>> entrySet = this.map.entrySet();
            builder.append("{");
            int i = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                Object val = entry.getValue();
                builder.append(this.quote(entry.getKey())).append(":");
                if (val instanceof String) {
                    builder.append(this.quote(String.valueOf(val)));
                } else if (val instanceof Integer) {
                    builder.append(Integer.valueOf(String.valueOf(val)));
                } else if (val instanceof Boolean) {
                    builder.append(val);
                } else if (val instanceof JSONObject) {
                    builder.append(val);
                } else if (val.getClass().isArray()) {
                    builder.append("[");
                    int len = Array.getLength(val);
                    for (int j = 0; j < len; ++j) {
                        builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
                    }
                    builder.append("]");
                }
                builder.append(++i == entrySet.size() ? "}" : ",");
            }
            return builder.toString();
        }

        private String quote(String string) {
            return "\"" + escape(string) + "\"";
        }

        private String escape(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"':  sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n");  break;
                    case '\r': sb.append("\\r");  break;
                    case '\t': sb.append("\\t");  break;
                    default:   sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    public static class EmbedObject {
        private String title;
        private String description;
        private String url;
        private Color color;
        private Footer footer;
        private Thumbnail thumbnail;
        private Image image;
        private Author author;
        private List<Field> fields = new ArrayList<Field>();

        public String getTitle() {
            return this.title;
        }

        public String getDescription() {
            return this.description;
        }

        public String getUrl() {
            return this.url;
        }

        public Color getColor() {
            return this.color;
        }

        public Footer getFooter() {
            return this.footer;
        }

        public Thumbnail getThumbnail() {
            return this.thumbnail;
        }

        public Image getImage() {
            return this.image;
        }

        public Author getAuthor() {
            return this.author;
        }

        public List<Field> getFields() {
            return this.fields;
        }

        public EmbedObject setTitle(String title) {
            this.title = title;
            return this;
        }

        public EmbedObject setDescription(String description) {
            this.description = description;
            return this;
        }

        public EmbedObject setUrl(String url) {
            this.url = url;
            return this;
        }

        public EmbedObject setColor(Color color) {
            this.color = color;
            return this;
        }

        public EmbedObject setFooter(String text, String icon) {
            this.footer = new Footer(text, icon);
            return this;
        }

        public EmbedObject setThumbnail(String url) {
            this.thumbnail = new Thumbnail(url);
            return this;
        }

        public EmbedObject setImage(String url) {
            this.image = new Image(url);
            return this;
        }

        public EmbedObject setAuthor(String name, String url, String icon) {
            this.author = new Author(name, url, icon);
            return this;
        }

        public EmbedObject addField(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        private static class Footer {
            private String text;
            private String iconUrl;

            private Footer(String text, String iconUrl) {
                this.text = text;
                this.iconUrl = iconUrl;
            }

            private String getText() {
                return this.text;
            }

            private String getIconUrl() {
                return this.iconUrl;
            }
        }

        private static class Thumbnail {
            private String url;

            private Thumbnail(String url) {
                this.url = url;
            }

            private String getUrl() {
                return this.url;
            }
        }

        private static class Image {
            private String url;

            private Image(String url) {
                this.url = url;
            }

            private String getUrl() {
                return this.url;
            }
        }

        private static class Author {
            private String name;
            private String url;
            private String iconUrl;

            private Author(String name, String url, String iconUrl) {
                this.name = name;
                this.url = url;
                this.iconUrl = iconUrl;
            }

            private String getName() {
                return this.name;
            }

            private String getUrl() {
                return this.url;
            }

            private String getIconUrl() {
                return this.iconUrl;
            }
        }

        private static class Field {
            private String name;
            private String value;
            private boolean inline;

            private Field(String name, String value, boolean inline) {
                this.name = name;
                this.value = value;
                this.inline = inline;
            }

            private String getName() {
                return this.name;
            }

            private String getValue() {
                return this.value;
            }

            private boolean isInline() {
                return this.inline;
            }
        }
    }
}
 