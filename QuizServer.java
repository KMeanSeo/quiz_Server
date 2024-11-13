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
        serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));
        serverGUI = new QuizServerGUI(this);
        serverGUI.setVisible(true);
    }

    // Method to start accepting client connections and handle them
    public void start() {
        System.out.println("Quiz Server started...");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connection accepted.");
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    // Update methods for the GUI
    public synchronized void updateClientStatus(String clientId, String status) {
        serverGUI.updateClientStatus(clientId, status);
    }

    public synchronized void updateClientScore(String clientId, int score) {
        serverGUI.updateClientScore(clientId, score);
    }

    // Main method to start the server
    public static void main(String[] args) {
        try {
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

        // Constructor for ClientHandler
        public ClientHandler(Socket socket, QuizServer server) throws IOException {
            this.socket = socket;
            this.server = server;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            clientId = UUID.randomUUID().toString();
            score = 0;
        }

        @Override
        public void run() {
            try {
                System.out.println("Client " + clientId + " connected.");
                server.updateClientStatus(clientId, "Connected");

                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("Received from client " + clientId + ": " + request); // 로그 추가

                    // 확인 로직과 디버깅 로그 추가
                    if (request.equals("CONNECT|SERVER")) {
                        out.println("200|Connection_Accepted|10"); // 10을 총 질문 수로 예시
                        out.flush();
                        System.out.println("Sent to client " + clientId + ": 200|Connection_Accepted|10");

                    } else if (request.equals("QUIZ|REQUEST")) {
                        out.println("201|Quiz_Content|Sample Question|1/10");
                        out.flush();
                        System.out.println("Sent to client " + clientId + ": 201|Quiz_Content|Sample Question|1/10");

                    } else if (request.startsWith("ANSWER|")) {
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

        // Simple answer check method for example purposes
        private boolean processAnswer(String answer) {
            return "correct".equalsIgnoreCase(answer);
        }
    }
}