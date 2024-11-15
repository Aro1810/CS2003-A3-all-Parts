import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChirplyServer {
    private int port;
    private String documentRoot;
    private Store storage;

    public ChirplyServer(int port, String documentRoot) {
        this.port = port;
        this.documentRoot = documentRoot;
        storage = new Store();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            loadChirps(documentRoot + "/chirps.json");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, documentRoot, storage)).start(); // Handle each client in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Configuration config = new Configuration("cs2003-C3.properties");
        ChirplyServer server = new ChirplyServer(config.serverPort_, config.documentRoot_);
        server.startServer();
    }

    public void loadChirps(String filePath) {
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
            storage.addFromJson(jsonString);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
