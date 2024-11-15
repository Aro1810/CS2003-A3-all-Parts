import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String documentRoot;
    private final Store storage;
    private final List<String> federatedServers;

    public ClientHandler(Socket clientSocket, String documentRoot, Store storage, List<String> federatedServers) {
        this.clientSocket = clientSocket;
        this.documentRoot = documentRoot;
        this.storage = storage;
        this.federatedServers = federatedServers;

        String clientIP = clientSocket.getInetAddress().getHostAddress();
        System.out.printf("Client connected: client IP %s\n", clientIP);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {
            handleClient(in, out);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.printf("Client disconnected: client IP %s\n", clientSocket.getInetAddress().getHostName());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void handleClient(BufferedReader in, OutputStream out) throws IOException {
        String requestLine;
        while ((requestLine = in.readLine()) != null && !requestLine.isEmpty()) {
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length >= 2) {
                String method = requestParts[0];
                String path = requestParts[1];
                int chirpID;
                
                switch (method) {
                    case "GET":
                        if (path.equals("/chirps")) {
                            handleGetChirps(out);
                        } else {
                            handleGetFile(path, out);
                        }
                        break;
                    case "POST":
                        if (path.equals("/chirps")) {
                            handlePostChirps(in, out);
                        }
                        break;
                    case "DELETE":
                        chirpID = getChirpId(path);
                        handleDeleteChirps(chirpID, out);
                        break;
                    case "PUT":
                        chirpID = getChirpId(path);
                        updateChirps(in, out, chirpID);
                        break;
                    default:
                        sendResponse(out, 404, "Not Found", "404 Not Found", "text/plain");
                        break;
                }
            } else {
                sendResponse(out, 400, "Bad Request", "Bad Request", "text/plain");
            }
        }
    }

    private void handleGetChirps(OutputStream out) throws IOException {
        JsonObject localChirps = storage.getAllChirpsAsJson();
        JsonArrayBuilder allChirps = javax.json.Json.createArrayBuilder();

        // Add local chirps
        JsonArray localChirpsArray = localChirps.getJsonArray("chirps");
        if (localChirpsArray != null) {
            for (JsonValue chirp : localChirpsArray) {
                allChirps.add(chirp); // Add each element to the builder
            }
        }

        // Fetch federated chirps
        for (String server : federatedServers) {
            try {
                URL url = new URL("http://" + server + "/chirps");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Add Via header for loop prevention
                conn.addRequestProperty("Via", "localhost:" + 24477); ///// it shouldn't be hardcoded maybe use my initial and port

                int status = conn.getResponseCode();
                if (status == 200) {
                    InputStream is = conn.getInputStream();
                    JsonArray federatedChirps = javax.json.Json.createReader(is).readArray();
                    for (JsonValue chirp : federatedChirps) {
                        allChirps.add(chirp); // Add each federated chirp
                    }
                }
            } catch (Exception e) {
                // Add an error chirp for failed servers
                allChirps.add(javax.json.Json.createObjectBuilder()
                    .add("id", -1)
                    .add("username", "Error")
                    .add("content", "Unable to fetch chirps from " + server)
                    .add("timestamp", LocalDateTime.now().toString()));
            }
        }

        // Combine and send response
        JsonObject responseJson = javax.json.Json.createObjectBuilder()
            .add("chirps", allChirps.build())
            .build();
        sendResponse(out, 200, "OK", responseJson.toString(), "application/json");
    }   

    private void handlePostChirps(BufferedReader in, OutputStream out) throws IOException {
        String line;
        int contentLength = 0;
    
        // Parse headers to find Content-Length
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }
        
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
    
        try {
            // Parse JSON body
            JsonObject requestBody = javax.json.Json.createReader(new StringReader(new String(body))).readObject();
    
            String username = requestBody.getString("username");
            String content = requestBody.getString("content");
    
            // Create and store the chirp
            Chirp chirp = new Chirp(storage.findNextChirpId(), username, content, LocalDateTime.now());
            storage.addChirp(chirp);
    
            // Send success response
            String responseBody = chirp.toJson().toString();
            sendResponse(out, 201, "Created", responseBody, "application/json");
    } catch (Exception e) {
            sendResponse(out, 400, "Bad Request", "Invalid JSON format", "text/plain");
        }
    }
    

    private void handleGetFile(String path, OutputStream out) throws IOException {
        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(documentRoot + path);
        if (file.exists() && file.isFile()) {
            serveFile(file, out);
        } else {
            sendResponse(out, 404, "Not Found", "404 Not Found", "text/plain");
        }
    }

    private void serveFile(File file, OutputStream out) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        long fileLength = file.length();
        sendResponseHeaders(out, 200, "OK", mimeType, fileLength);

        BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(file));
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = fileInput.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
        fileInput.close();
    }

    private void sendResponseHeaders(OutputStream out, int statusCode, String statusMessage, String contentType, long contentLength) throws IOException {
        String headers = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                         "Content-Type: " + contentType + "\r\n" +
                         "Content-Length: " + contentLength + "\r\n\r\n";
        out.write(headers.getBytes());
    }

    private void sendResponse(OutputStream out, int statusCode, String statusMessage, String body, String contentType) throws IOException {
        String headers = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                         "Content-Type: " + contentType + "\r\n" +
                         "Content-Length: " + body.length() + "\r\n\r\n";
        out.write(headers.getBytes());
        out.write(body.getBytes());
        out.flush();
    }

    private void handleDeleteChirps(int chirpID, OutputStream out) throws IOException {
        if (storage.getChirp(chirpID) != null) {
            storage.deleteChirp(chirpID);
            sendResponse(out, 200, "OK", "Chirp deleted successfully", "text/plain");
        } else {
            sendResponse(out, 404, "Not Found", "Chirp not found", "text/plain");
        }
    }
    

    private int getChirpId(String path) {
        String regex = "^/chirps/([^/]+)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(path);

        if(matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }   



    private void updateChirps(BufferedReader in, OutputStream out, int chirpID) throws IOException {
        String line;
        int contentLength = 0;
    
        // Parse headers to find Content-Length
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }
        
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
    
        try {
            // Parse JSON body
            JsonObject requestBody = javax.json.Json.createReader(new StringReader(new String(body))).readObject();
    
            String username = requestBody.getString("username");
            String content = requestBody.getString("content");
    
            // Create and store the chirp
            Chirp chirp = new Chirp(chirpID, username, content, LocalDateTime.now());
            storage.updateChirp(chirpID, chirp);
    
            // Send success response
            String responseBody = chirp.toJson().toString();
            sendResponse(out, 201, "updated", responseBody, "application/json");
        } catch (Exception e) {
            sendResponse(out, 400, "Bad Request", "Invalid JSON format", "text/plain");
        }

    }
}

