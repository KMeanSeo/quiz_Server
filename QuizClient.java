import java.io.*;
import java.net.*;

public class QuizClient {
    // Server address and port number for the connection
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 7777;

    // Socket, input and output streams for communication
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Constructor to establish the connection to the server
    public QuizClient() throws IOException {
        // Create a socket to connect to the server at the specified address and port
        socket = new Socket(SERVER_ADDRESS, PORT);

        // Initialize the input stream to read data from the server
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Initialize the output stream to send data to the server
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    // Method to send a connection request to the server
    public String connectToServer() throws IOException {
        // Send a message to the server to establish a connection
        out.println("CONNECT|SERVER");

        // Read and return the response from the server
        return in.readLine();
    }

    // Method to request a quiz from the server
    public String requestQuiz() throws IOException {
        // Send a request to the server for a quiz
        out.println("QUIZ|REQUEST");

        // Read and return the quiz question from the server
        return in.readLine();
    }

    // Method to send the user's answer to the server
    public String sendAnswer(String userAnswer) throws IOException {
        // Send the user's answer to the server
        out.println("ANSWER|" + userAnswer);

        // Read and return the server's response (correct or incorrect)
        return in.readLine();
    }

    // Method to receive a response from the server (e.g., feedback or next
    // question)
    public String receiveResponse() throws IOException {
        // Read and return the server's response
        return in.readLine();
    }

    // Method to close the connection to the server
    public void closeConnection() throws IOException {
        // Close the socket connection to the server
        socket.close();
    }
}