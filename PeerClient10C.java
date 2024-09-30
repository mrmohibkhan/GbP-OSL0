import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class PeerClient10C {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // Address of the central server
    private static final int SERVER_PORT = 5000; // Port of the central server
    private static List<ClientInfo> peerList = new ArrayList<>();
    private static int peerServerPort; // Port on which this client listens for peers

    public static void main(String[] args) {
        MainApp obj = new MainApp();
        if (args.length != 1) {
            System.out.println("Usage: java PeerClient <port>");
            return;
        }

        peerServerPort = Integer.parseInt(args[0]); // Client will bind to this port

        try {
            // Record the time right before sending the request to the server
            LocalDateTime requestSentTime = LocalDateTime.now();

            // Step 1: Connect to the central server
            Socket serverSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to the central server");

            // Send this client's listening port to the server
            PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true);
            serverOut.println("PORT:" + peerServerPort); // Send something like "PORT:6001"
            System.out.println("Sent listening port " + peerServerPort + " to the server");

            // Step 2: Read the broadcasted client list from the server
            BufferedReader input = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String clientList;
            System.out.println("Waiting to receive client list from server...");

            // Record the time when the client receives the client list
            LocalDateTime listReceivedTime = null;

            // Read until the list broadcast starts
            while ((clientList = input.readLine()) != null) {
                System.out.println(clientList);
                if (clientList.startsWith("Connected Clients:")) {
                    // Mark the time when the client list is received
                    listReceivedTime = LocalDateTime.now();
                    break;
                }
            }

            // Step 3: Calculate the time difference between request and list received for each client
            if (listReceivedTime != null) {
                Duration timeDiff = Duration.between(requestSentTime, listReceivedTime);
                System.out.println("Time difference between sending request and receiving the list: " + timeDiff.toMillis() + " milliseconds");
            }

            // Step 4: Parse the client list received from the server
            int idCounter = 1;
            while ((clientList = input.readLine()) != null && !clientList.isEmpty()) {
                String[] parts = clientList.split(":");
                String peerIp = parts[0];
                int peerPort = Integer.parseInt(parts[1]);
                peerList.add(new ClientInfo(idCounter++, peerIp, peerPort)); // Assign each peer a unique ID
            }
            System.out.println("Client list received. Ready to connect to peers.");

            // Step 5: Start a thread to listen for incoming peer connections on the specified port
            ServerSocket peerServerSocket = new ServerSocket(peerServerPort);
            System.out.println("Listening for peer connections on port " + peerServerPort);

            new Thread(() -> {
                try {
                    while (true) {
                        Socket peerSocket = peerServerSocket.accept();
                        System.out.println("Connected to peer: " + peerSocket.getInetAddress());

                        new Thread(new PeerHandler(peerSocket)).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Step 6: Allow the user to generate a random number and decide to broadcast or forward the message
            Scanner scanner = new Scanner(System.in);
            String message;
            Random random = new Random();

            while (true) {
                System.out.println("\nList of peers:");
                for (ClientInfo peer : peerList) {
                    System.out.println(peer.getId() + ". " + peer.getIpAddress() + ":" + peer.getPort());
                }

                System.out.print("\nEnter your message (or 'exit' to quit): ");
                message = scanner.nextLine();

                if (message.equalsIgnoreCase("exit")) break;

                // Generate a random number
                int randomNumber = random.nextInt(100); // Generate a random number between 0 and 99
                System.out.println("Generated random number: " + randomNumber);

                if (randomNumber % 2 == 0) {
                    // Even number: Broadcast to all peers
                    System.out.println("Broadcasting message to all peers...");
                    broadcastMessageToAll("BROADCAST:0:" + message); // 0 means no forwards
                } else {
                    // Odd number: Forward to a random peer with counter 0 (since it's the first forward)
                    forwardMessageToRandomPeer("FORWARD:1:" + message); // 1 indicates the first forward
                }
            }

            serverSocket.close();

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    // Helper method to broadcast a message to all peers
    private static void broadcastMessageToAll(String message) {
        for (ClientInfo peer : peerList) {
            try {
                Socket peerSocket = new Socket(peer.getIpAddress(), peer.getPort());
                PrintWriter peerOut = new PrintWriter(peerSocket.getOutputStream(), true);
                peerOut.println(message);
                peerSocket.close();
                System.out.println("Message broadcasted to peer " + peer.getId());
            } catch (IOException e) {
                System.out.println("Error broadcasting message to peer: " + peer.getIpAddress() + ":" + peer.getPort());
            }
        }
    }

    // Helper method to forward a message to a random peer with a delay and incremented counter
    private static void forwardMessageToRandomPeer(String message) {
        Random random = new Random();
        int delay = 100 + random.nextInt(91); // Random delay between 100-190 milliseconds
        ClientInfo randomPeer = peerList.get(random.nextInt(peerList.size()));
        System.out.println("Forwarding message to random peer: " + randomPeer.getIpAddress() + ":" + randomPeer.getPort());

        try {
            // Apply delay before forwarding the message
            Thread.sleep(delay);
            System.out.println("Delaying forwarding for " + delay + " milliseconds.");

            // Increment the forward counter before forwarding the message
            String[] messageParts = message.split(":");
            int forwardCount = Integer.parseInt(messageParts[1]) + 1; // Increment the counter
            String updatedMessage = "FORWARD:" + forwardCount + ":" + messageParts[2]; // Update the message with the new counter

            Socket peerSocket = new Socket(randomPeer.getIpAddress(), randomPeer.getPort());
            PrintWriter peerOut = new PrintWriter(peerSocket.getOutputStream(), true);
            peerOut.println(updatedMessage);
            peerSocket.close();
            System.out.println("Message forwarded to peer " + randomPeer.getId() + " with counter: " + forwardCount);
        } catch (IOException e) {
            System.out.println("Error forwarding message to peer: " + randomPeer.getIpAddress() + ":" + randomPeer.getPort());
        } catch (InterruptedException e) {
            System.out.println("Error: Thread interrupted during sleep.");
        }
    }

    // Helper class to handle peer information
    static class ClientInfo {
        private int id; // Unique ID for each peer
        private String ipAddress;
        private int port;

        public ClientInfo(int id, String ipAddress, int port) {
            this.id = id;
            this.ipAddress = ipAddress;
            this.port = port;
        }

        public int getId() {
            return id;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }
    }

    // Handler class to manage incoming peer-to-peer connections
    static class PeerHandler implements Runnable {
        private Socket peerSocket;
        public MainApp obj1 = new MainApp();
        private Random random = new Random();

        public PeerHandler(Socket socket) {
            this.peerSocket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                String message;

                while ((message = reader.readLine()) != null) {
                    System.out.println("Message from peer: " + message);

                    // Check if it's a broadcast message or a forwarded message
                    if (message.startsWith("BROADCAST:")) {
                        // It's a broadcast message, call obj1.abc()
                        String actualMessage = message.substring(message.indexOf(':', 10) + 1); // Extract the actual message
                        int forwardCount = Integer.parseInt(message.split(":")[1]);
                        System.out.println("Received broadcast message: " + actualMessage);
                        System.out.println("Message was forwarded " + forwardCount + " times before being broadcast.");
                        obj1.abc(actualMessage, 10);  // Call obj1.abc when a broadcast is received
                    } else if (message.startsWith("FORWARD:")) {
                        // It's a forwarded message, extract the counter and message
                        String[] messageParts = message.split(":");
                        int forwardCount = Integer.parseInt(messageParts[1]);
                        String actualMessage = messageParts[2];

                        System.out.println("Received forwarded message: " + actualMessage + " with forward count: " + forwardCount);

                        // Generate a random number and decide what to do with the message
                        int randomNumber = random.nextInt(100);
                        System.out.println("Generated random number (on receive): " + randomNumber);

                        if (randomNumber % 2 == 0) {
                            // Even number: Broadcast the message to all peers
                            System.out.println("Even number received, broadcasting message to all peers...");
                            broadcastMessageToAll("BROADCAST:" + forwardCount + ":" + actualMessage);
                        } else {
                            // Odd number: Forward the message to a random peer with incremented counter
                            forwardMessageToRandomPeer("FORWARD:" + (forwardCount + 1) + ":" + actualMessage);
                        }
                    }
                }

            } catch (IOException ex) {
                System.out.println("Peer handler exception: " + ex.getMessage());
            }
        }

        // Use the broadcast method directly within the PeerHandler
        private void broadcastMessageToAll(String message) {
            for (ClientInfo peer : peerList) {
                try {
                    Socket peerSocket = new Socket(peer.getIpAddress(), peer.getPort());
                    PrintWriter peerOut = new PrintWriter(peerSocket.getOutputStream(), true);
                    peerOut.println(message);
                    peerSocket.close();
                    System.out.println("Message broadcasted to peer " + peer.getId());
                } catch (IOException e) {
                    System.out.println("Error broadcasting message to peer: " + peer.getIpAddress() + ":" + peer.getPort());
                }
            }
        }

        // Use the forward method within the PeerHandler for forwarding to a random peer with incremented counter
        private void forwardMessageToRandomPeer(String message) {
            ClientInfo randomPeer = peerList.get(random.nextInt(peerList.size()));
            System.out.println("Forwarding message to random peer: " + randomPeer.getIpAddress() + ":" + randomPeer.getPort());
            try {
                Socket peerSocket = new Socket(randomPeer.getIpAddress(), randomPeer.getPort());
                PrintWriter peerOut = new PrintWriter(peerSocket.getOutputStream(), true);
                peerOut.println(message);
                peerSocket.close();
                System.out.println("Message forwarded to peer " + randomPeer.getId());
            } catch (IOException e) {
                System.out.println("Error forwarding message to peer: " + randomPeer.getIpAddress() + ":" + randomPeer.getPort());
            }
        }
    }
}
