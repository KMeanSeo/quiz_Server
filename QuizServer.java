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
                System.out.println("New client connection accepted."); // 로그 추가

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
                System.out.println("Client " + clientId + " connected.");
                server.updateClientStatus(clientId, "Connected");

                String request;
                // Listen for incoming requests from the client
                while ((request = in.readLine()) != null) {
                    System.out.println("Received from client " + clientId + ": " + request);

                    // Handle the CONNECT|SERVER request from the client
                    if (request.equals("CONNECT|SERVER")) {
                        out.println("200|Connection_Accepted|10"); // 10은 예시로 설정한 총 질문 수
                        out.flush();
                        System.out.println("Sent to client " + clientId + ": 200|Connection_Accepted|10");

                    } else if (request.equals("QUIZ|REQUEST")) {
                        // Handle quiz request and send a sample question
                        out.println("201|Quiz_Content|Sample Question|1/10");
                        out.flush();
                        System.out.println("Sent to client " + clientId + ": 201|Quiz_Content|Sample Question|1/10");

                    } else if (request.startsWith("ANSWER|")) {
                        // Handle answer submission
                        boolean correct = processAnswer(request.substring(7));
                        if (correct) {
                            score++;
                            out.println("202|Correct_Answer");
                            System.out.println("Sent to client " + clientId + ": 202|Correct_Answer");
                        } else {
                            out.println("203|Wrong_Answer");
                            System.out.println("Sent to client " + clientId + ": 203|Wrong_Answer");
                        }
                        out.flush();
                        server.updateClientScore(clientId, score);

                    } else {
                        // Handle unrecognized requests
                        System.out.println("Unrecognized message from client " + clientId + ": " + request);
                    }
                }
                System.out.println("Client " + clientId + " disconnected.");
            } catch (IOException e) {
                System.err.println("Error communicating with client " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
                server.updateClientStatus(clientId, "Disconnected");
            }
        }

        // Method to process the client's answer
        private boolean processAnswer(String answer) {
            return "correct".equalsIgnoreCase(answer);
        }
    }
}