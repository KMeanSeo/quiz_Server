import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class QuizServer {
    private String serverAddress;
    private int port;
    private static final String QUIZ_FILE = "quiz_list.csv";
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private List<QuizQuestion> quizQuestions = new ArrayList<>();
    private QuizServerGUI serverGUI;

    // constructor to initialize the server
    public QuizServer() {
        loadServerAddress();
        initializeServer();
    }

    // loads the server address and port from address.dat file
    private void loadServerAddress() {
        try (BufferedReader br = new BufferedReader(new FileReader("address.dat"))) {
            serverAddress = br.readLine().trim();
            port = Integer.parseInt(br.readLine().trim());
            System.out.println("Loaded server address: " + serverAddress + ", port: " + port);
        } catch (IOException e) {
            System.err.println("Failed to load server address from address.dat");
            e.printStackTrace();
        }
    }

    // initializes server and GUI
    private void initializeServer() {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Initializing GUI...");
            serverGUI = new QuizServerGUI(this);
            serverGUI.setVisible(true);
            System.out.println("GUI initialized successfully.");
        });

        new Thread(() -> {
            try {
                System.out.println("Initializing server socket...");
                serverSocket = new ServerSocket(port, 50, InetAddress.getByName(serverAddress));
                System.out.println("Server socket created. Listening on " + serverAddress + ":" + port);
                appendStatusMessage("Server started on " + serverAddress + ":" + port);

                loadQuizQuestions();
                start();
            } catch (IOException e) {
                System.err.println("Failed to initialize server socket: " + e.getMessage());
                appendStatusMessage("Failed to initialize server socket: " + e.getMessage());
            }
        }).start();
    }

    // starts server to accept client connection
    public void start() {
        System.out.println("Quiz Server started...");
        while (true) {
            try {
                System.out.println("Waiting for client connections...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connection accepted: " + clientSocket);
                appendStatusMessage("New client connection accepted: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
                appendStatusMessage("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    // load quiz questions from quiz_list.csv file
    private void loadQuizQuestions() {
        try (BufferedReader br = new BufferedReader(new FileReader(QUIZ_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.contains("\"")
                        ? new String[] { line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")),
                                line.split(",")[1].trim() }
                        : line.split(",", 2);

                if (parts.length >= 2) {
                    quizQuestions.add(new QuizQuestion(parts[0].trim(), parts[1].trim()));
                }
            }
            System.out.println("Loaded " + quizQuestions.size() + " quiz questions successfully.");
            appendStatusMessage("Loaded " + quizQuestions.size() + " quiz questions successfully.");
        } catch (IOException e) {
            System.err.println("Error loading quiz questions: " + e.getMessage());
            appendStatusMessage("Error loading quiz questions: " + e.getMessage());
        }
    }

    // return random subset of quiz question
    public synchronized List<QuizQuestion> getRandomQuestions(int numberOfQuestions) {
        Collections.shuffle(quizQuestions);
        return quizQuestions.subList(0, Math.min(numberOfQuestions, quizQuestions.size()));
    }

    // update client status in GUI
    public synchronized void updateClientStatus(String clientId, String status) {
        SwingUtilities.invokeLater(() -> {
            serverGUI.updateClientStatus(clientId, status);
            serverGUI.refreshClientPanel();
        });
    }

    // update client score in GUI
    public synchronized void updateClientScore(String clientId, int score) {
        SwingUtilities.invokeLater(() -> {
            serverGUI.updateClientScore(clientId, score);
            serverGUI.refreshClientPanel();
        });
    }

    // updates client progress in GUI
    public synchronized void updateClientProgress(String clientId, int currentQuestion, int totalQuestions) {
        SwingUtilities.invokeLater(() -> {
            serverGUI.updateClientProgress(clientId, currentQuestion, totalQuestions);
            serverGUI.refreshClientPanel();
        });
    }

    // appends status message server log at GUI
    public synchronized void appendStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            serverGUI.appendStatusMessage(message);
        });
    }

    // main method to launch server
    public static void main(String[] args) {
        new QuizServer();
    }

    // inner class to handle client connections
    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientId;
        private int score;
        private QuizServer server;
        private List<QuizQuestion> selectedQuestions;
        private int currentQuestionIndex = 0;

        // constructor to initialize client handler
        public ClientHandler(Socket socket, QuizServer server) throws IOException {
            this.socket = socket;
            this.server = server;

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            clientId = UUID.randomUUID().toString();
            score = 0;
            selectedQuestions = server.getRandomQuestions(10);

            SwingUtilities.invokeLater(() -> {
                serverGUI.addClient(clientId, selectedQuestions.size());
                serverGUI.updateClientStatus(clientId, "Connected");
                serverGUI.updateClientScore(clientId, score);
                serverGUI.updateClientProgress(clientId, currentQuestionIndex + 1, selectedQuestions.size());
            });

            appendStatusMessage("Client " + clientId + " connected.");
        }

        @Override
        public void run() {
            try {
                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("Received from client " + clientId + ": " + request);
                    server.appendStatusMessage("Received from client " + clientId + ": " + request);

                    if (request.equals("CONNECT|SERVER")) {
                        out.println("200|Connection_Accepted|" + selectedQuestions.size());
                        out.flush();
                        server.appendStatusMessage("Sent to client " + clientId + ": 200|Connection_Accepted|"
                                + selectedQuestions.size());
                    } else if (request.equals("QUIZ|REQUEST")) {
                        handleQuizRequest();
                    } else if (request.startsWith("ANSWER|")) {
                        handleAnswer(request);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error communicating with client " + clientId + ": " + e.getMessage());
                server.appendStatusMessage("Error communicating with client " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    server.updateClientStatus(clientId, "Disconnected");
                    server.appendStatusMessage("Client " + clientId + " disconnected.");
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                    server.appendStatusMessage("Error closing client socket: " + e.getMessage());
                }
            }
        }

        // handles quiz requests from client
        private void handleQuizRequest() {
            if (currentQuestionIndex >= selectedQuestions.size()) {
                String finalScore = score + "/" + selectedQuestions.size();
                out.println("501|Final_Score|" + finalScore);
                server.appendStatusMessage("Sent to client " + clientId + ": 501|Final_Score|" + finalScore);
                return;
            }

            QuizQuestion currentQuestion = selectedQuestions.get(currentQuestionIndex++);
            String response = "301|Quiz_Content|" + currentQuestion.getQuestion() + "|" + currentQuestionIndex + "/"
                    + selectedQuestions.size();
            out.println(response);
            server.appendStatusMessage("Sent to client " + clientId + ": " + response);
            server.updateClientProgress(clientId, currentQuestionIndex, selectedQuestions.size());
        }

        // handles answer from client
        private void handleAnswer(String request) {
            String answer = request.substring(7);
            boolean correct = selectedQuestions.get(currentQuestionIndex - 1).isCorrectAnswer(answer);

            if (correct)
                score++;

            String response = correct ? "401|Correct_Answer" : "402|Wrong_Answer";
            out.println(response);
            server.appendStatusMessage("Sent to client " + clientId + ": " + response);
            server.updateClientScore(clientId, score);
        }
    }

    // inner class to represent a quiz question
    public static class QuizQuestion {
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