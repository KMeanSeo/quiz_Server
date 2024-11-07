import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class QuizServer {
    private static final int MAX_QUESTIONS = 10; // Set the maximum number of questions per client
    private static int TOTAL_QUESTIONS;
    private static List<String[]> quizList;

    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(7777);
        System.out.println("The quiz server is running...\n");
        ExecutorService pool = Executors.newFixedThreadPool(1);

        String filePath = "quiz_list.csv";
        quizList = NetworkQuiz.loadQuiz(filePath);
        TOTAL_QUESTIONS = quizList.size();
        System.out.println("Total Questions Loaded: " + TOTAL_QUESTIONS + "\n");

        while (true) {
            Socket sock = listener.accept();
            try {
                pool.execute(new QuizHandler(sock));
            } catch (RejectedExecutionException e) {
                System.out.println("Thread limit exceeded, unable to handle new connections.\n");
                try (PrintWriter out = new PrintWriter(sock.getOutputStream(), true)) {
                    out.println("503|Service_Unavailable");
                } catch (IOException ex) {
                    System.out.println("Error sending service unavailable message to client: " + ex.getMessage() + "\n");
                } finally {
                    sock.close();
                }
            }
        }
    }

    private static class QuizHandler implements Runnable {
        private Socket socket;
        private List<String[]> selectedQuestions;
        private int currentQuestionIndex = 0; // Track current question index for the client
        private int correctAnswersCount = 0;
        private int currentScore = 0; // Track the current score

        // Calculate the points per question based on the total number of questions
        private final int pointsPerQuestion = 100 / MAX_QUESTIONS;

        QuizHandler(Socket socket) {
            this.socket = socket;
            this.selectedQuestions = getRandomQuestions(MAX_QUESTIONS); // Select random questions for each client
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
                        out.println("400|Connection_Failed");
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
            out.println("200|Connection_Accepted|" + MAX_QUESTIONS);
            System.out.println("Sent: 200|Connection_Accepted|" + MAX_QUESTIONS + "\n");
        }

        private void handleQuizRequest(PrintWriter out) {
            // Check if all questions have been asked
            if (currentQuestionIndex >= selectedQuestions.size()) {
                int totalScore = calculateScore();
                out.println("204|Final_Score|" + totalScore);  // Send final score if no more questions
                return;
            }

            // Otherwise, send the next question
            String[] quiz = selectedQuestions.get(currentQuestionIndex);
            currentQuestionIndex++;
            out.println("201|Quiz_Content|" + quiz[0] + "|" + currentQuestionIndex + "/" + MAX_QUESTIONS);
        }

        private void handleAnswer(String request, PrintWriter out) {
            String[] parts = request.split("\\|", 2);
            if (parts.length < 2) {
                out.println("400|Connection_Failed");
                return;
            }

            String userAnswer = parts[1];
            String correctAnswer = selectedQuestions.get(currentQuestionIndex - 1)[1];

            if (isCorrectAnswer(userAnswer, correctAnswer)) {
                correctAnswersCount++;
                currentScore += pointsPerQuestion; // Increase score for a correct answer
                out.println("202|Correct_Answer|Score:" + currentScore);
            } else {
                out.println("203|Wrong_Answer|Score:" + currentScore);
            }

            // Send final score and close connection if this was the last question
            if (currentQuestionIndex == MAX_QUESTIONS) {
                int totalScore = calculateScore();
                out.println("204|Final_Score|" + totalScore);
                try {
                    socket.close();  // Close the connection after sending final score
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
            // Normalize user answer and correct answer by trimming and converting to lowercase
            String normalizedUserAnswer = userAnswer.trim().toLowerCase();
            String normalizedCorrectAnswer = correctAnswer.trim().toLowerCase();
        
            // Check if normalized answers are equal
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
                // Split the line using a more robust CSV parser to handle commas within quotes
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
                inQuotes = !inQuotes; // Toggle inQuotes state
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0); // Clear the current builder
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim()); // Add the last value
        return values.toArray(new String[0]);
    }
}
