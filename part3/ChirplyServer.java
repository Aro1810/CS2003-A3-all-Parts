import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ChirplyServer {
    protected int port;
    private String documentRoot;
    private Store storage;
    private List<String> federatedServers;

    public ChirplyServer(int port, String documentRoot, List<String> federatedServers) {
        this.port = port;
        this.documentRoot = documentRoot;
        this.federatedServers = federatedServers;
        storage = new Store();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            loadChirps(documentRoot + "/chirps.json");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, documentRoot, storage, federatedServers)).start(); // Handle each client in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Configuration config = new Configuration("cs2003-C3.properties");
        List<String> federatedServers = Arrays.asList(config.federation_.split(","));
        ChirplyServer server = new ChirplyServer(config.serverPort_, config.documentRoot_, federatedServers);
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
