import java.io.*;
import java.net.*;

public class QuizClient {
    // Server address and port number for the connection
    private static final String SERVER_ADDRESS = "192.168.1.10"; // 서버 IP
    private static final int PORT = 7777;

    // Socket, input and output streams for communication
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Constructor to establish the connection to the server
    public QuizClient() throws IOException {
        try {
            // Create a socket to connect to the server at the specified address and port
            socket = new Socket(SERVER_ADDRESS, PORT);

            // Initialize the input stream to read data from the server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Initialize the output stream to send data to the server
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to the server at " + SERVER_ADDRESS + ":" + PORT);
        } catch (IOException e) {
            System.err.println("Failed to connect to the server at " + SERVER_ADDRESS + ":" + PORT);
            throw e;
        }
    }

    // Method to send a connection request to the server
    public String connectToServer() throws IOException {
        // Send a message to the server to establish a connection
        out.println("CONNECT|SERVER");
        out.flush(); // Ensure data is sent immediately
        System.out.println("Sent to server: CONNECT|SERVER");

        // Read and return the response from the server
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // Method to request a quiz from the server
    public String requestQuiz() throws IOException {
        // Send a request to the server for a quiz
        out.println("QUIZ|REQUEST");
        out.flush();
        System.out.println("Sent to server: QUIZ|REQUEST");

        // Read and return the quiz question from the server
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // Method to send the user's answer to the server
    public String sendAnswer(String userAnswer) throws IOException {
        // Send the user's answer to the server
        out.println("ANSWER|" + userAnswer);
        out.flush();
        System.out.println("Sent to server: ANSWER|" + userAnswer);

        // Read and return the server's response (correct or incorrect)
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // Method to receive a response from the server (e.g., feedback or next
    // question)
    public String receiveResponse() throws IOException {
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // Method to close the connection to the server
    public void closeConnection() throws IOException {
        System.out.println("Closing connection to server...");
        socket.close();
    }

    // Main method to test the connection
    public static void main(String[] args) {
        try {
            QuizClient client = new QuizClient();
            String response = client.connectToServer();
            System.out.println("Server response: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}