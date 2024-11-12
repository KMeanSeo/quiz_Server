import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QuizServerGUI extends JFrame {
    // Text area to display general server status or messages
    private JTextArea statusArea;

    // Maps to store labels for client status and client score based on client ID
    private Map<String, JLabel> clientStatusLabels;
    private Map<String, JLabel> clientScoreLabels;

    // Constructor to initialize the server monitor GUI
    public QuizServerGUI(QuizServer server) {
        // Set window title, size, and default close operation
        setTitle("Quiz Server Monitor");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize the maps to hold client status and score labels
        clientStatusLabels = new HashMap<>();
        clientScoreLabels = new HashMap<>();

        // Initialize the text area to display server messages
        statusArea = new JTextArea();
        statusArea.setEditable(false); // Make the text area non-editable
        add(new JScrollPane(statusArea), BorderLayout.CENTER); // Add text area to the center of the window

        // Create a panel for displaying client statuses and scores
        JPanel clientPanel = new JPanel();
        clientPanel.setLayout(new GridLayout(0, 2)); // Layout with 2 columns (client ID, status and score)
        add(clientPanel, BorderLayout.SOUTH); // Add panel to the bottom of the window

        // Example of adding two clients
        addClient("Client1");
        addClient("Client2");
    }

    // Method to add a new client to the GUI (with client ID, status, and score
    // labels)
    public void addClient(String clientId) {
        // Create labels for client status and score
        JLabel statusLabel = new JLabel("Status: Disconnected");
        JLabel scoreLabel = new JLabel("Score: 0");

        // Store the labels in their respective maps, using client ID as the key
        clientStatusLabels.put(clientId, statusLabel);
        clientScoreLabels.put(clientId, scoreLabel);

        // Get the client panel and add the client ID, status label, and score label
        JPanel clientPanel = (JPanel) getContentPane().getComponent(1);
        clientPanel.add(new JLabel(clientId));
        clientPanel.add(statusLabel);
        clientPanel.add(scoreLabel);
    }

    // Method to update the status of a client in the GUI
    public void updateClientStatus(String clientId, String status) {
        // Ensure thread safety when updating the GUI
        SwingUtilities.invokeLater(() -> {
            JLabel statusLabel = clientStatusLabels.get(clientId);
            if (statusLabel != null) {
                // Update the status label if client already exists
                statusLabel.setText("Status: " + status);
            } else {
                // Add the client if it doesn't exist in the map
                addClient(clientId);
                clientStatusLabels.get(clientId).setText("Status: " + status);
            }
        });
    }

    // Method to update the score of a client in the GUI
    public void updateClientScore(String clientId, int score) {
        // Ensure thread safety when updating the GUI
        SwingUtilities.invokeLater(() -> {
            JLabel scoreLabel = clientScoreLabels.get(clientId);
            if (scoreLabel != null) {
                // Update the score label if client already exists
                scoreLabel.setText("Score: " + score);
            } else {
                // Add the client if it doesn't exist in the map
                addClient(clientId);
                clientScoreLabels.get(clientId).setText("Score: " + score);
            }
        });
    }

    // Main method to launch the server and GUI
    public static void main(String[] args) {
        // Run the server and GUI initialization on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            QuizServer server = null;
            try {
                // Initialize and start the QuizServer
                server = new QuizServer();
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Initialize and display the server GUI
            QuizServerGUI serverGUI = new QuizServerGUI(server);
            serverGUI.setVisible(true);
        });
    }
}