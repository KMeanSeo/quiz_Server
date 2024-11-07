import java.io.*;
import java.net.*;

public class QuizClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 7777;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public QuizClient() throws IOException {
        socket = new Socket(SERVER_ADDRESS, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String connectToServer() throws IOException {
        out.println("CONNECT|SERVER");
        return in.readLine();
    }

    public String requestQuiz() throws IOException {
        out.println("QUIZ|REQUEST");
        return in.readLine();
    }

    public String sendAnswer(String userAnswer) throws IOException {
        out.println("ANSWER|" + userAnswer);
        return in.readLine();
    }

    public void closeConnection() throws IOException {
        socket.close();
    }
}
