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

    // Panel to display client information
    private JPanel clientPanel;

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
        clientPanel = new JPanel();
        clientPanel.setLayout(new GridLayout(0, 3, 10, 10)); // 3 columns: Client ID, Status, Score
        add(new JScrollPane(clientPanel), BorderLayout.SOUTH); // Add panel to the bottom of the window
    }

    // Method to add a new client to the GUI (with client ID, status, and score
    // labels)
    public void addClient(String clientId) {
        if (clientStatusLabels.containsKey(clientId)) {
            // If client already exists, no need to add again
            return;
        }

        // Create labels for client status and score
        JLabel idLabel = new JLabel(clientId);
        JLabel statusLabel = new JLabel("Status: Disconnected");
        JLabel scoreLabel = new JLabel("Score: 0");

        // Store the labels in their respective maps, using client ID as the key
        clientStatusLabels.put(clientId, statusLabel);
        clientScoreLabels.put(clientId, scoreLabel);

        // Add the client ID, status label, and score label to the panel
        clientPanel.add(idLabel);
        clientPanel.add(statusLabel);
        clientPanel.add(scoreLabel);

        // Refresh the layout to display the new client
        clientPanel.revalidate();
        clientPanel.repaint();
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

    // Method to display general server status messages
    public void appendStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength()); // Auto-scroll
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