import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Server10B {
    private static final int PORT = 5000;
    private static final int BATCH_SIZE = 10; // Number of clients per batch
    private static List<ClientInfo> clients = new ArrayList<>();
    private static LocalDateTime lastClientConnectedTime;

    public static void main(String[] args) {
        // Use an object for synchronization
        final Object lock = new Object();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                // Reset the clients list for each batch of clients
                clients = new ArrayList<>();

                // Accept clients until we reach the batch size
                while (clients.size() < BATCH_SIZE) {
                    Socket socket = serverSocket.accept();
                    lastClientConnectedTime = LocalDateTime.now(); // Log time when client connects

                    // Handle the client in a new thread
                    new Thread(new ClientHandler(socket, lock)).start();

                    // Wait until the client has been added to the list
                    synchronized (lock) {
                        while (clients.size() < BATCH_SIZE) {
                            lock.wait();
                            break;
                        }
                    }
                }

                // After receiving clients equal to BATCH_SIZE, broadcast the client list
                System.out.println(BATCH_SIZE + " clients connected. Broadcasting client list...");

                // Calculate the time difference between the last client and the broadcast
                LocalDateTime broadcastTime = LocalDateTime.now();
                Duration timeDiff = Duration.between(lastClientConnectedTime, broadcastTime);
                System.out.println("Time difference between last client connected and broadcast: " + timeDiff.toMillis() + " milliseconds");

                // Broadcast the list to the clients
                broadcastClientList();

                System.out.println("Server is ready to accept more clients.");
            }

        } catch (IOException | InterruptedException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void broadcastClientList() {
        StringBuilder clientList = new StringBuilder();
        for (ClientInfo client : clients) {
            clientList.append(client.getIpAddress())
                      .append(":").append(client.getPort())
                      .append("\n");
        }

        // Broadcast the client list to all clients and close their sockets
        for (ClientInfo client : clients) {
            try {
                Socket socket = client.getSocket();
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println("Connected Clients: \n" + clientList.toString());
                System.out.println("Sent client list to " + client.getIpAddress() + ":" + client.getPort());

                // Close the client socket after broadcasting
                socket.close();
            } catch (IOException e) {
                System.out.println("Error broadcasting to client: " + client.getIpAddress() + ":" + client.getPort());
            }
        }

        System.out.println("Client list broadcasted to all clients.");
    }

    // Inner class to handle client connection and communication
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Object lock;

        public ClientHandler(Socket socket, Object lock) {
            this.clientSocket = socket;
            this.lock = lock;
        }

        @Override
        public void run() {
            try {
                // Get the client's listening port
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientMessage = reader.readLine(); // Expecting something like "PORT:6001"
                System.out.println("Received from client: " + clientMessage);

                String[] messageParts = clientMessage.split(":");
                if (messageParts[0].equals("PORT")) {
                    int clientPort = Integer.parseInt(messageParts[1]);
                    String clientIp = clientSocket.getInetAddress().getHostAddress();

                    // Add the client info to the list in a thread-safe manner
                    synchronized (lock) {
                        clients.add(new ClientInfo(clientSocket, clientIp, clientPort));
                        System.out.println("Client connected from: " + clientIp + " on port: " + clientPort);

                        // Notify the main thread that a client has been added
                        lock.notifyAll();
                    }
                }

            } catch (IOException ex) {
                System.out.println("Client handler exception: " + ex.getMessage());
            }
        }
    }

    // Class to store client information
    private static class ClientInfo {
        private Socket socket;
        private String ipAddress;
        private int port;

        public ClientInfo(Socket socket, String ipAddress, int port) {
            this.socket = socket;
            this.ipAddress = ipAddress;
            this.port = port;
        }

        public Socket getSocket() {
            return socket;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }
    }
}
