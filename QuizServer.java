import java.io.*;
import java.net.*;
import java.util.*;

public class QuizServer {
    // Port number the server will listen on
    private static final int PORT = 7777;
    private static final String QUIZ_FILE = "quiz_list.csv"; // CSV 파일 경로

    // Server socket to accept client connections
    private ServerSocket serverSocket;

    // List to hold all connected clients
    private List<ClientHandler> clients = new ArrayList<>();

    // List to hold quiz questions and answers
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    // GUI for the server to monitor client status and scores
    private QuizServerGUI serverGUI;

    // Constructor to initialize the server socket and GUI
    public QuizServer() throws IOException {
        // Create server socket to listen on the specified port using IPv4
        serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));

        // Initialize the server's GUI
        serverGUI = new QuizServerGUI(this);

        // Load quiz questions from CSV file
        loadQuizQuestions();

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
                System.out.println("New client connection accepted.");

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

    // Method to load quiz questions from a CSV file
    private void loadQuizQuestions() {
        try (BufferedReader br = new BufferedReader(new FileReader(QUIZ_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Check if the line contains quotes to handle comma within quotes
                String[] parts;
                if (line.contains("\"")) {
                    int quoteIndex1 = line.indexOf("\"");
                    int quoteIndex2 = line.indexOf("\"", quoteIndex1 + 1);
                    String question = line.substring(quoteIndex1 + 1, quoteIndex2);
                    String answer = line.substring(quoteIndex2 + 2).trim();
                    parts = new String[] { question, answer };
                } else {
                    parts = line.split(",", 2); // Split normally if no quotes
                }

                if (parts.length >= 2) {
                    String question = parts[0].trim();
                    String answer = parts[1].trim();
                    quizQuestions.add(new QuizQuestion(question, answer));
                }
            }
            System.out.println("Loaded " + quizQuestions.size() + " quiz questions.");
        } catch (IOException e) {
            System.err.println("Error loading quiz questions: " + e.getMessage());
        }
    }

    // Method to get a list of 10 random quiz questions
    private List<QuizQuestion> getRandomQuestions(int count) {
        List<QuizQuestion> questions = new ArrayList<>(quizQuestions);
        Collections.shuffle(questions);
        return questions.subList(0, Math.min(count, questions.size()));
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
        private List<QuizQuestion> selectedQuestions;
        private int currentQuestionIndex = 0;

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

            // Select 10 random quiz questions for this client
            selectedQuestions = server.getRandomQuestions(10);
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
                        out.println("200|Connection_Accepted|" + selectedQuestions.size());
                        out.flush();
                        System.out.println(
                                "Sent to client " + clientId + ": 200|Connection_Accepted|" + selectedQuestions.size());

                    } else if (request.equals("QUIZ|REQUEST")) {
                        // Send the next question in the selected questions
                        if (currentQuestionIndex < selectedQuestions.size()) {
                            QuizQuestion currentQuestion = selectedQuestions.get(currentQuestionIndex);
                            out.println("201|Quiz_Content|" + currentQuestion.getQuestion() + "|"
                                    + (currentQuestionIndex + 1) + "/" + selectedQuestions.size());
                            System.out.println(
                                    "Sent to client " + clientId + ": 201|Quiz_Content|" + currentQuestion.getQuestion()
                                            + "|" + (currentQuestionIndex + 1) + "/" + selectedQuestions.size());
                        } else {
                            out.println("204|Final_Score|" + score); // Send final score when no more questions
                            System.out.println("Sent to client " + clientId + ": 204|Final_Score|" + score);
                        }
                        out.flush();

                    } else if (request.startsWith("ANSWER|")) {
                        // Handle answer submission
                        String answer = request.substring(7);
                        if (currentQuestionIndex < selectedQuestions.size()) {
                            QuizQuestion currentQuestion = selectedQuestions.get(currentQuestionIndex);
                            boolean correct = currentQuestion.isCorrectAnswer(answer);
                            if (correct) {
                                score++;
                                out.println("202|Correct_Answer");
                                System.out.println("Sent to client " + clientId + ": 202|Correct_Answer");
                            } else {
                                out.println("203|Wrong_Answer");
                                System.out.println("Sent to client " + clientId + ": 203|Wrong_Answer");
                            }
                            currentQuestionIndex++; // Move to the next question
                        } else {
                            out.println("204|Final_Score|" + score);
                            System.out.println("Sent to client " + clientId + ": 204|Final_Score|" + score);
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
    }

    // Inner class to represent a quiz question
    private static class QuizQuestion {
        private String question;
        private String answer;

        public QuizQuestion(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public boolean isCorrectAnswer(String userAnswer) {
            return answer.equalsIgnoreCase(userAnswer.trim());
        }
    }
}