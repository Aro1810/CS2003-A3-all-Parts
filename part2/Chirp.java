import javax.json.Json;
import javax.json.JsonObject;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a social media post, called a "chirp", containing an ID, username, content, and timestamp.
 */
public class Chirp {
    private int id;
    private String username;
    private String content;
    private LocalDateTime posted_at;

    /**
     * Constructs a new {@code Chirp} instance.
     *
     * @param id       The unique identifier of the chirp.
     * @param username The username of the person posting the chirp.
     * @param content  The content of the chirp.
     * @param postedAt The date and time the chirp was posted.
     */
    public Chirp(int id, String username, String content, LocalDateTime postedAt) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.posted_at = postedAt;
    }

    /**
     * Returns the unique identifier of the chirp.
     *
     * @return The chirp's ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the username of the person who posted the chirp.
     *
     * @return The chirp's username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the content of the chirp.
     *
     * @return The chirp's content.
     */
    public String getContent() {
        return content;
    }


    /**
     * Returns the date and time when the chirp was posted.
     *
     * @return The chirp's posting time as a {@code LocalDateTime}.
     */
    public LocalDateTime getPostedAt() {
        return posted_at;
    }

    /**
     * Creates a {@code Chirp} instance from a JSON object.
     *
     * @param jsonObject The {@code JsonObject} containing chirp data.
     * @return A new {@code Chirp} instance populated with data from the JSON object.
     */
    public static Chirp fromJson(JsonObject jsonObject) {
        int id = jsonObject.getInt("id");
        String username = jsonObject.getString("username");
        String content = jsonObject.getString("content");
        String postedAtStr = jsonObject.getString("posted_at");
        LocalDateTime postedAt = LocalDateTime.parse(postedAtStr, DateTimeFormatter.ISO_DATE_TIME);
        return new Chirp(id, username, content, postedAt);
    }

    /**
     * Creates a {@code Chirp} instance from a string containing JSON.
     *
     * @param jsonString A JSON-formatted string representing a chirp.
     * @return A new {@code Chirp} instance populated with data from the JSON string.
     */
    public static Chirp fromJson(String jsonString) {
        StringReader stringReader = new StringReader(jsonString);
        JsonObject jsonObject = Json.createReader(stringReader).readObject();
        return fromJson(jsonObject);
    }

    /**
     * Converts this {@code Chirp} to a JSON object.
     *
     * @return A {@code JsonObject} representation of the chirp.
     */
    public JsonObject toJsonObject() {
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("id", id)
                .add("username", username)
                .add("content", content)
                .add("posted_at", posted_at.format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        return jsonObject;
    }

    /**
     * Converts this {@code Chirp} to a JSON string.
     *
     * @return A JSON-formatted string representation of the chirp.
     */
    public String toJson() {
        return toJsonObject().toString();
    }
}