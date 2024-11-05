import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class QuizServer {
    private static int TOTAL_QUESTIONS;
    private static List<String[]> quizList;

    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(7777);
        System.out.println("The quiz server is running...\n");
        ExecutorService pool = Executors.newFixedThreadPool(1);

        String filePath = "network_quiz.csv";
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
        private Set<Integer> askedQuestions = new HashSet<>();
        private int correctAnswers = 0;
        private int questionsRequested = 0;
        private int userRequestedQuestions = 0; 

        QuizHandler(Socket socket) {
            this.socket = socket;
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
                        handleConnect(out, in);
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

        private void handleConnect(PrintWriter out, Scanner in) throws IOException {
            out.println("200|Connection_Accepted|" + TOTAL_QUESTIONS);
            System.out.println("Sent: 200|Connection_Accepted|" + TOTAL_QUESTIONS + "\n");

            if (in.hasNextLine()) {
                String request = in.nextLine();
                String[] parts = request.split("\\|");
                if (parts[0].equals("QUESTIONS") && parts.length == 2) {
                    try {
                        userRequestedQuestions = Integer.parseInt(parts[1]);
                        if (userRequestedQuestions <= 0 || userRequestedQuestions > TOTAL_QUESTIONS) {
                            userRequestedQuestions = TOTAL_QUESTIONS;
                        }
                        out.println("You will be given " + userRequestedQuestions + " questions.");
                    } catch (NumberFormatException e) {
                        out.println("400|Invalid_Number_of_Questions");
                    }
                } else {
                    out.println("400|Connection_Failed");
                }
            }
        }

        private void handleQuizRequest(PrintWriter out) {
            if (askedQuestions.size() >= userRequestedQuestions) {
                out.println("404|Quiz_Not_Found");
                return;
            }

            Random rand = new Random();
            int questionNumber;

            do {
                questionNumber = rand.nextInt(TOTAL_QUESTIONS) + 1;
            } while (askedQuestions.contains(questionNumber));

            askedQuestions.add(questionNumber);
            questionsRequested++;

            String[] quiz = quizList.get(questionNumber - 1);
            
            out.println("201|Quiz_Content|" + quiz[0] + "|" + questionsRequested + "/" + userRequestedQuestions);
        }

        private void handleAnswer(String request, PrintWriter out) {
            String[] parts = request.split("\\|");
            if (parts.length < 3) {
                out.println("400|Connection_Failed");
                return;
            }

            int questionNumber;
            try {
                questionNumber = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                out.println("400|Connection_Failed");
                return;
            }

            if (!askedQuestions.contains(questionNumber)) {
                out.println("404|Quiz_Not_Found");
                return;
            }

            String userAnswer = parts[1];
            String correctAnswer = quizList.get(questionNumber - 1)[1];

            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                out.println("202|Correct_Answer");
                correctAnswers++;
            } else {
                out.println("203|Wrong_Answer");
            }

            if (askedQuestions.size() == userRequestedQuestions) {
                int totalScore = calculateScore();
                out.println("204|Final_Score|" + totalScore);
            }
        }

        private int calculateScore() {
            return (int) ((double) correctAnswers / userRequestedQuestions * 100);
        }
    }
}

class NetworkQuiz {
    public static List<String[]> loadQuiz(String filePath) {
        List<String[]> quizList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] quiz = line.split(",");
                if (quiz.length >= 2) {
                    quizList.add(quiz);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading quiz file: " + e.getMessage() + "\n");
        }
        return quizList;
    }
}
