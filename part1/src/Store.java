import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a collection of {@code Chirp} objects, allowing for storage, retrieval,
 * addition, updating, and deletion of chirps.
 */
public class Store {
    private Map<Integer, Chirp> chirpStore = new HashMap<>();

    /**
     * Adds a new {@code Chirp} to the store.
     *
     * @param chirp The {@code Chirp} object to be added.
     */
    public void addChirp(Chirp chirp) {
        chirpStore.put(chirp.getId(), chirp);
    }

    /**
     * Creates and adds a new {@code Chirp} to the store with the specified username and content.
     * The chirp ID is auto-generated, and the current time is used as the posting time.
     *
     * @param username The username of the person creating the chirp.
     * @param content  The content of the chirp.
     */
    public void addChirp(String username, String content) {
        int nextId = findNextChirpId();
        LocalDateTime postedAt = LocalDateTime.now();
        Chirp newChirp = new Chirp(nextId, username, content, postedAt);
        addChirp(newChirp);
    }


    /**
     * Finds the next available chirp ID by identifying the highest ID currently in use and adding one.
     *
     * @return The next available chirp ID.
     */
    public int findNextChirpId() {
        int highestId = chirpStore.keySet().stream().max(Integer::compareTo).orElse(-1);
        return highestId + 1;
    }

    /**
     * Retrieves a {@code Chirp} from the store by its ID.
     *
     * @param id The ID of the chirp to retrieve.
     * @return The {@code Chirp} with the specified ID, or {@code null} if no chirp exists with that ID.
     */
    public Chirp getChirp(int id) {
        return chirpStore.get(id);
    }

    /**
     * Updates an existing chirp in the store with the specified ID. Throws an exception if the chirp doesn't exist.
     *
     * @param id    The ID of the chirp to update.
     * @param chirp The {@code Chirp} object with updated data.
     */
    public void updateChirp(int id, Chirp chirp) {
        if (chirpStore.containsKey(id)) {
            chirpStore.put(id, chirp);
        } else {
            throw new IllegalArgumentException("Attempt to update no existent chirp.");
        }
    }

    /**
     * Deletes a {@code Chirp} from the store by its ID.
     *
     * @param id The ID of the chirp to delete.
     * @return The deleted {@code Chirp} object, or {@code null} if no chirp exists with that ID.
     */
    public Chirp deleteChirp(int id) {
        return chirpStore.remove(id);
    }


    /**
     * Retrieves all chirps currently in the store.
     *
     * @return A list of all {@code Chirp} objects in the store.
     */
    public List<Chirp> getAllChirps() {
        return new ArrayList<>(chirpStore.values());
    }

    /**
     * Adds multiple chirps to the store from a JSON string. The JSON string should contain
     * an array of chirps under the key "chirps".
     *
     * @param jsonString A JSON-formatted string representing an array of chirps.
     */
    public void addFromJson(String jsonString) {
        StringReader stringReader = new StringReader(jsonString);
        JsonObject jsonObject = Json.createReader(stringReader).readObject();
        jsonObject.getJsonArray("chirps").forEach(jsonValue -> {
            JsonObject chirpJson = jsonValue.asJsonObject();
            Chirp chirp = Chirp.fromJson(chirpJson);
            addChirp(chirp);
        });
    }

    /**
     * Retrieves all chirps and returns a JSON string representing an
     * array of these chirps.
     * 
     * @return A JSON object containing all chirps in an array named "chirps".
     */
    public JsonObject getAllChirpsAsJson() {
        // first we create an array from the chirps
        JsonArrayBuilder chirpArray = Json.createArrayBuilder();
        for (Chirp c : getAllChirps()) {
            chirpArray.add(c.toJsonObject());
        }
        // and then we build an object with the array named "chirps"
        JsonObject chirpJson = Json.createObjectBuilder()
            .add("chirps", chirpArray)
            .build();
        return chirpJson;
    }
}

