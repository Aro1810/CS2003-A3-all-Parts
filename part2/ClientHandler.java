import javax.json.JsonObject;
import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String documentRoot;
    private final Store storage;

    public ClientHandler(Socket clientSocket, String documentRoot, Store storage) {
        this.clientSocket = clientSocket;
        this.documentRoot = documentRoot;
        this.storage = storage;

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
        JsonObject chirpsJson = storage.getAllChirpsAsJson();
        String jsonString = chirpsJson.toString();
        sendResponse(out, 200, "OK", jsonString, "application/json");
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

