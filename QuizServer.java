import java.io.*;
import java.net.*;
import java.util.*;

public class QuizServer {
    // Port number the server will listen on
    private static final int PORT = 7777;

    // Server socket to accept client connections
    private ServerSocket serverSocket;

    // List to hold all connected clients
    private List<ClientHandler> clients = new ArrayList<>();

    // GUI for the server to monitor client status and scores
    private QuizServerGUI serverGUI;

    // Constructor to initialize the server socket and GUI
    public QuizServer() throws IOException {
        // Create server socket to listen on the specified port using IPv4
        serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));

        // Initialize the server's GUI
        serverGUI = new QuizServerGUI(this);

        // Make the server GUI visible
        serverGUI.setVisible(true);
    }

    // Method to start accepting client connections and handle them
    public void start() {
        System.out.println("Quiz Server started...");
        while (true) {
            try {
                // Accept a new client connection
                Socket clientSocket = serverSocket.accept();

                // Create a ClientHandler to manage communication with the client
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);

                // Add the client handler to the list of clients
                clients.add(clientHandler);

                // Start a new thread for the client handler
                new Thread(clientHandler).start();
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    // Method to update client status in the GUI
    public synchronized void updateClientStatus(String clientId, String status) {
        serverGUI.updateClientStatus(clientId, status);
    }

    // Method to update client score in the GUI
    public synchronized void updateClientScore(String clientId, int score) {
        serverGUI.updateClientScore(clientId, score);
    }

    // Main method to start the server
    public static void main(String[] args) {
        try {
            // Create and start the QuizServer
            QuizServer server = new QuizServer();
            server.start();
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    // ClientHandler class to handle communication with each client
    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientId;
        private int score;
        private QuizServer server;

        // Constructor to initialize the client handler with socket and server reference
        public ClientHandler(Socket socket, QuizServer server) throws IOException {
            this.socket = socket;
            this.server = server;

            // Set up input and output streams for communication with the client
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Generate a unique client ID for this client
            clientId = UUID.randomUUID().toString();

            // Initialize score to 0
            score = 0;
        }

        // Method that runs on a separate thread to handle client communication
        @Override
        public void run() {
            try {
                // Update client status to "Connected" in the server GUI
                server.updateClientStatus(clientId, "Connected");

                String request;
                // Listen for incoming requests from the client
                while ((request = in.readLine()) != null) {
                    if (request.startsWith("ANSWER|")) {
                        // Process the answer and update the score
                        boolean correct = processAnswer(request.substring(7));
                        if (correct) {
                            score++; // Increment score if answer is correct
                            out.println("202|Correct_Answer");
                        } else {
                            out.println("203|Wrong_Answer");
                        }
                        // Update the client score in the GUI
                        server.updateClientScore(clientId, score);
                    } else if (request.equals("QUIZ|REQUEST")) {
                        // Send a quiz question to the client
                        out.println("201|Quiz_Content|Sample Question|1/10");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error communicating with client: " + e.getMessage());
            } finally {
                try {
                    // Close the socket connection
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
                // Update client status to "Disconnected" in the server GUI
                server.updateClientStatus(clientId, "Disconnected");
            }
        }

        // Method to process the client's answer
        private boolean processAnswer(String answer) {
            // For simplicity, we assume the correct answer is "correct"
            return "correct".equalsIgnoreCase(answer);
        }
    }
}