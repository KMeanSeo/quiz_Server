import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizServer {
    private static final int MAX_QUESTIONS = 10; 
    private static int TOTAL_QUESTIONS;
    private static List<String[]> quizList;

    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(7777);
        System.out.println("The quiz server is running...\n");

        ExecutorService pool = Executors.newFixedThreadPool(20);

        String filePath = "quiz_list.csv";
        quizList = NetworkQuiz.loadQuiz(filePath);
        TOTAL_QUESTIONS = quizList.size();
        System.out.println("Total Questions Loaded: " + TOTAL_QUESTIONS + "\n");

        while (true) {
            Socket sock = listener.accept();
            pool.execute(new QuizHandler(sock));
        }
    }

    private static class QuizHandler implements Runnable {
        private Socket socket;
        private List<String[]> selectedQuestions;
        private int currentQuestionIndex = 0; 
        private int correctAnswersCount = 0;
        private int currentScore = 0; 

        private final int pointsPerQuestion = 100 / MAX_QUESTIONS;

        QuizHandler(Socket socket) {
            this.socket = socket;
            this.selectedQuestions = getRandomQuestions(MAX_QUESTIONS); 
        }

        @Override
        public void run() {
            System.out.println("Connected: " + socket + "\n");
            try (Scanner in = new Scanner(socket.getInputStream());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                while (in.hasNextLine()) {
                    String request = in.nextLine();
                    System.out.println("Received: " + request + "\n");

                    if (request.equals("CONNECT|SERVER")) {
                        handleConnect(out);
                    } else if (request.startsWith("QUIZ|REQUEST")) {
                        handleQuizRequest(out);
                    } else if (request.startsWith("ANSWER|")) {
                        handleAnswer(request, out);
                    } else {
                        String response = "400|Connection_Failed";
                        out.println(response);
                        System.out.println("Sent: " + response + "\n");
                    }
                }
            } catch (Exception e) {
                System.out.println("Error handling client request: " + e.getMessage() + "\n");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage() + "\n");
                }
                System.out.println("Closed: " + socket + "\n");
            }
        }

        private void handleConnect(PrintWriter out) throws IOException {
            String response = "200|Connection_Accepted|" + MAX_QUESTIONS;
            out.println(response);
            System.out.println("Sent: " + response + "\n");
        }

        private void handleQuizRequest(PrintWriter out) {
            if (currentQuestionIndex >= selectedQuestions.size()) {
                int totalScore = calculateScore();
                String response = "204|Final_Score|" + totalScore;
                out.println(response);
                System.out.println("Sent: " + response + "\n");
                return;
            }

            String[] quiz = selectedQuestions.get(currentQuestionIndex);
            currentQuestionIndex++;
            String response = "201|Quiz_Content|" + quiz[0] + "|" + currentQuestionIndex + "/" + MAX_QUESTIONS;
            out.println(response);
            System.out.println("Sent: " + response + "\n");
        }

        private void handleAnswer(String request, PrintWriter out) {
            String[] parts = request.split("\\|", 2);
            if (parts.length < 2) {
                String response = "400|Connection_Failed";
                out.println(response);
                System.out.println("Sent: " + response + "\n");
                return;
            }

            String userAnswer = parts[1];
            String correctAnswer = selectedQuestions.get(currentQuestionIndex - 1)[1];

            String response;
            if (isCorrectAnswer(userAnswer, correctAnswer)) {
                correctAnswersCount++;
                currentScore += pointsPerQuestion; 
                response = "202|Correct_Answer|Score:" + currentScore;
            } else {
                response = "203|Wrong_Answer|Score:" + currentScore;
            }

            out.println(response);
            System.out.println("Sent: " + response + "\n");

            if (currentQuestionIndex == MAX_QUESTIONS) {
                int totalScore = calculateScore();
                response = "204|Final_Score|" + totalScore;
                out.println(response);
                System.out.println("Sent: " + response + "\n");
                try {
                    socket.close(); 
                } catch (IOException e) {
                    System.out.println("Error closing socket after sending final score: " + e.getMessage());
                }
            }
        }

        private int calculateScore() {
            return currentScore;
        }

        private List<String[]> getRandomQuestions(int numQuestions) {
            List<String[]> randomQuestions = new ArrayList<>(quizList);
            Collections.shuffle(randomQuestions);
            return randomQuestions.subList(0, Math.min(numQuestions, randomQuestions.size()));
        }

        private boolean isCorrectAnswer(String userAnswer, String correctAnswer) {
            String normalizedUserAnswer = userAnswer.trim().toLowerCase();
            String normalizedCorrectAnswer = correctAnswer.trim().toLowerCase();
            return normalizedUserAnswer.equals(normalizedCorrectAnswer);
        }
    }
}

class NetworkQuiz {
    public static List<String[]> loadQuiz(String filePath) {
        List<String[]> quizList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] quiz = parseCSVLine(line);
                if (quiz.length >= 2) {
                    quizList.add(quiz);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading quiz file: " + e.getMessage() + "\n");
        }
        return quizList;
    }

    private static String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }
}
