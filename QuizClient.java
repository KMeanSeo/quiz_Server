import java.io.*;
import java.net.*;

public class QuizClient {
    private String serverAddress;
    private int port;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // constructor to initialize client and connect to server
    public QuizClient() throws IOException {
        loadServerAddress();
        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to the server at " + serverAddress + ":" + port);
        } catch (IOException e) {
            System.err.println("Failed to connect to the server at " + serverAddress + ":" + port);
            throw e;
        }
    }

    // load server address and port from address.dat file
    private void loadServerAddress() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader("address.dat"))) {
            serverAddress = br.readLine().trim();
            port = Integer.parseInt(br.readLine().trim());
            System.out.println("Loaded server address: " + serverAddress + ", port: " + port);
        } catch (IOException e) {
            System.err.println("Failed to load server address from address.dat");
            throw e;
        }
    }

    // send connection request to server
    public String connectToServer() throws IOException {
        out.println("CONNECT|SERVER");
        out.flush();
        System.out.println("Sent to server: CONNECT|SERVER");
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // request quiz question from server
    public String requestQuiz() throws IOException {
        out.println("QUIZ|REQUEST");
        out.flush();
        System.out.println("Sent to server: QUIZ|REQUEST");
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // send user answer to server
    public String sendAnswer(String userAnswer) throws IOException {
        out.println("ANSWER|" + userAnswer);
        out.flush();
        System.out.println("Sent to server: ANSWER|" + userAnswer);
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // receive a response from serer
    public String receiveResponse() throws IOException {
        String response = in.readLine();
        System.out.println("Received from server: " + response);
        return response;
    }

    // close connection to server
    public void closeConnection() throws IOException {
        System.out.println("Closing connection to server...");
        socket.close();
    }

    // main method launch client
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