import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class QuizServer {
    private static final int PORT = 7777; // 서버 포트 번호
    private static final String QUIZ_FILE = "quiz_list.csv"; // 퀴즈 CSV 파일 경로
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>(); // 연결된 클라이언트 리스트
    private List<QuizQuestion> quizQuestions = new ArrayList<>(); // 퀴즈 문제와 답 리스트
    private QuizServerGUI serverGUI;

    public QuizServer() {
        initializeServer(); // 서버 초기화 메서드 호출
    }

    // 서버 소켓 초기화 및 GUI 설정을 별도 스레드에서 실행
    private void initializeServer() {
        // GUI를 이벤트 디스패치 스레드에서 실행
        SwingUtilities.invokeLater(() -> {
            System.out.println("Initializing GUI...");
            serverGUI = new QuizServerGUI(this);
            serverGUI.setVisible(true);
            System.out.println("GUI initialized successfully.");
        });

        // 서버 소켓 초기화는 메인 스레드 또는 별도의 스레드에서 실행
        new Thread(() -> {
            try {
                System.out.println("Initializing server socket...");
                serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));
                System.out.println("Server socket created. Listening on port " + PORT);

                loadQuizQuestions(); // 퀴즈 CSV 파일에서 문제 불러오기

                // 클라이언트 연결 대기
                start();
            } catch (IOException e) {
                System.err.println("Failed to initialize server socket on port " + PORT + ": " + e.getMessage());
            }
        }).start();
    }

    // 서버 시작 메서드 - 클라이언트 연결 대기
    public void start() {
        System.out.println("Quiz Server started...");
        while (true) {
            try {
                System.out.println("Waiting for client connections...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connection accepted: " + clientSocket);

                // 클라이언트 핸들러 생성 및 스레드 시작
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    // 퀴즈 질문과 답을 CSV 파일에서 불러오는 메서드
    private void loadQuizQuestions() {
        try (BufferedReader br = new BufferedReader(new FileReader(QUIZ_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts;
                if (line.contains("\"")) { // 인용문이 포함된 질문 처리
                    int quoteIndex1 = line.indexOf("\"");
                    int quoteIndex2 = line.indexOf("\"", quoteIndex1 + 1);
                    String question = line.substring(quoteIndex1 + 1, quoteIndex2);
                    String answer = line.substring(quoteIndex2 + 2).trim();
                    parts = new String[] { question, answer };
                } else {
                    parts = line.split(",", 2);
                }
                if (parts.length >= 2) {
                    quizQuestions.add(new QuizQuestion(parts[0].trim(), parts[1].trim()));
                }
            }
            System.out.println("Loaded " + quizQuestions.size() + " quiz questions successfully.");
        } catch (IOException e) {
            System.err.println("Error loading quiz questions from file " + QUIZ_FILE + ": " + e.getMessage());
        }
    }

    // 주어진 수의 랜덤 퀴즈 질문을 반환하는 메서드
    public synchronized List<QuizQuestion> getRandomQuestions(int numberOfQuestions) {
        Collections.shuffle(quizQuestions);
        return quizQuestions.subList(0, Math.min(numberOfQuestions, quizQuestions.size()));
    }

    // GUI에서 클라이언트 상태를 업데이트하는 메서드
    public synchronized void updateClientStatus(String clientId, String status) {
        SwingUtilities.invokeLater(() -> serverGUI.updateClientStatus(clientId, status));
    }

    public synchronized void updateClientScore(String clientId, int score) {
        SwingUtilities.invokeLater(() -> serverGUI.updateClientScore(clientId, score));
    }

    public synchronized void updateClientProgress(String clientId, int currentQuestion, int totalQuestions) {
        SwingUtilities.invokeLater(() -> serverGUI.updateClientProgress(clientId, currentQuestion, totalQuestions));
    }

    public static void main(String[] args) {
        new QuizServer();
    }

    // 클라이언트와의 통신을 처리하는 클래스
    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientId;
        private int score;
        private QuizServer server;
        private List<QuizQuestion> selectedQuestions;
        private int currentQuestionIndex = 0;

        public ClientHandler(Socket socket, QuizServer server) throws IOException {
            this.socket = socket;
            this.server = server;

            System.out.println("Setting up client handler for client: " + socket);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            clientId = UUID.randomUUID().toString();
            score = 0;
            selectedQuestions = server.getRandomQuestions(10);

            serverGUI.addClient(clientId, selectedQuestions.size());
            server.updateClientStatus(clientId, "Connected");
            server.updateClientScore(clientId, score);
            server.updateClientProgress(clientId, currentQuestionIndex + 1, selectedQuestions.size());
            System.out.println("Client handler set up completed for client: " + clientId);
        }

        @Override
        public void run() {
            try {
                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("Received from client " + clientId + ": " + request);

                    if (request.equals("CONNECT|SERVER")) {
                        out.println("200|Connection_Accepted|" + selectedQuestions.size());
                        out.flush();
                    } else if (request.equals("QUIZ|REQUEST")) {
                        handleQuizRequest();
                    } else if (request.startsWith("ANSWER|")) {
                        handleAnswer(request);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error communicating with client " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                        System.out.println("Closed connection for client: " + clientId);
                    }
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
                server.updateClientStatus(clientId, "Disconnected");
            }
        }

        private void handleQuizRequest() {
            if (currentQuestionIndex >= selectedQuestions.size()) {
                out.println("204|Final_Score|" + score);
                server.updateClientStatus(clientId, "Quiz Completed");
                System.out.println("Sent final score to client " + clientId + ": " + score);
                return;
            }

            QuizQuestion currentQuestion = selectedQuestions.get(currentQuestionIndex);
            currentQuestionIndex++;
            String response = "201|Quiz_Content|" + currentQuestion.getQuestion() + "|" + currentQuestionIndex + "/"
                    + selectedQuestions.size();
            out.println(response);
            server.updateClientProgress(clientId, currentQuestionIndex, selectedQuestions.size());
            System.out.println("Sent quiz question to client " + clientId + ": " + currentQuestion.getQuestion());
        }

        private void handleAnswer(String request) {
            String answer = request.substring(7);
            QuizQuestion currentQuestion = selectedQuestions.get(currentQuestionIndex - 1);
            boolean correct = currentQuestion.isCorrectAnswer(answer);

            String response;
            if (correct) {
                score++;
                response = "202|Correct_Answer";
            } else {
                response = "203|Wrong_Answer";
            }
            out.println(response);
            server.updateClientScore(clientId, score);
            System.out.println(
                    "Client " + clientId + " answered question. Correct: " + correct + ", Current score: " + score);
        }
    }

    // 퀴즈 문제 및 답을 관리하는 내부 클래스
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