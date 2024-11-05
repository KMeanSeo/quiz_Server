import java.io.*;
import java.net.*;
import java.util.Scanner;

public class QuizClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 7777;
    private static int totalQuestions = 0;
    private static int questionsToAttempt = 0;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            connectToServer(out, in, scanner);

            for (int i = 1; i <= questionsToAttempt; i++) {
                requestQuiz(out, in);
                sendAnswer(out, in, i, scanner);
            }

        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void connectToServer(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        out.println("CONNECT|SERVER");
        System.out.println("Sent: CONNECT|SERVER");

        String response = in.readLine();
        System.out.println("Received: " + response);

        if (response.startsWith("200|Connection_Accepted")) {
            String[] parts = response.split("\\|");
            totalQuestions = Integer.parseInt(parts[2]);
            System.out.println("Total Questions Available: " + totalQuestions);

            System.out.print("Enter the number of questions you want to attempt (1 to " + totalQuestions + "): ");
            questionsToAttempt = Integer.parseInt(scanner.nextLine());

            out.println("QUESTIONS|" + questionsToAttempt);
            response = in.readLine();
            System.out.println("Received: " + response);
        } else {
            System.out.println("Failed to connect to server.");
        }
    }

    private static void requestQuiz(PrintWriter out, BufferedReader in) throws IOException {
        out.println("QUIZ|REQUEST");
        System.out.println("Sent: QUIZ|REQUEST");

        String quizResponse = in.readLine();
        if (quizResponse.startsWith("201|Quiz_Content")) {
            String[] parts = quizResponse.split("\\|");
            String question = parts[2];
            String progress = parts[3];
            
            System.out.println("\nQuestion: " + question);
            System.out.println("Progress: (" + progress + ")\n");
        } else {
            System.out.println("Error receiving question: " + quizResponse);
        }
    }

    private static void sendAnswer(PrintWriter out, BufferedReader in, int questionNumber, Scanner scanner) throws IOException {
        System.out.print("Enter your answer for question " + questionNumber + ": ");
        String userAnswer = scanner.nextLine();

        out.println("ANSWER|" + userAnswer + "|" + questionNumber);
        System.out.println("Sent: ANSWER|" + userAnswer + "|" + questionNumber);

        String answerResponse = in.readLine();
        if (answerResponse.startsWith("202|Correct_Answer")) {
            System.out.println("Received: Correct Answer!\n");
        } else if (answerResponse.startsWith("203|Wrong_Answer")) {
            System.out.println("Received: Wrong Answer.\n");
        } else {
            System.out.println("Received: " + answerResponse);
        }

        if (questionNumber == questionsToAttempt) {
            String finalScoreResponse = in.readLine();
            System.out.println("Final Score: " + finalScoreResponse);
        }
    }
}
