import java.io.*;
import java.net.*;
import java.util.Scanner;

public class QuizClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 7777;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            connectToServer(out, in);

            while (true) {
                String response = requestQuiz(out, in);
                
                // If the final score is received, exit the loop and close the connection
                if (response.startsWith("204|Final_Score")) {
                    System.out.println("Final Score: " + response.split("\\|")[2]);
                    break;
                }
                
                sendAnswer(out, in, scanner);
            }

            System.out.println("Quiz finished. Connection will now close.");

        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void connectToServer(PrintWriter out, BufferedReader in) throws IOException {
        out.println("CONNECT|SERVER");
        System.out.println("Sent: CONNECT|SERVER");

        String response = in.readLine();
        System.out.println("Received: " + response);

        if (!response.startsWith("200|Connection_Accepted")) {
            System.out.println("Failed to connect to server.");
        }
    }

    private static String requestQuiz(PrintWriter out, BufferedReader in) throws IOException {
        out.println("QUIZ|REQUEST");
        System.out.println("Sent: QUIZ|REQUEST");

        String quizResponse = in.readLine();
        if (quizResponse.startsWith("201|Quiz_Content")) {
            String[] parts = quizResponse.split("\\|", 4);

            if (parts.length >= 4) {
                String question = parts[2];
                String progress = parts[3];
                
                System.out.println("\nQuestion: " + question);
                System.out.println("Progress: (" + progress + ")\n");
            } else {
                System.out.println("Error: Incomplete question response - " + quizResponse);
            }
        } else if (quizResponse.startsWith("204|Final_Score")) {
            // Return final score response to handle in main loop
            return quizResponse;
        } else {
            System.out.println("Error receiving question: " + quizResponse);
        }
        return quizResponse;
    }

    private static void sendAnswer(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        System.out.print("Enter your answer: ");
        String userAnswer = scanner.nextLine();

        out.println("ANSWER|" + userAnswer);
        System.out.println("Sent: ANSWER|" + userAnswer);

        String answerResponse = in.readLine();
        if (answerResponse.startsWith("202|Correct_Answer")) {
            System.out.println("Received: Correct Answer!\n");
        } else if (answerResponse.startsWith("203|Wrong_Answer")) {
            System.out.println("Received: Wrong Answer.\n");
        } else {
            System.out.println("Received: " + answerResponse);
        }
    }
}
