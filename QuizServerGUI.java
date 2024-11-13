import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class QuizServerGUI extends JFrame {
    // Text area to display general server status or messages
    private JTextArea statusArea;

    // Maps to store labels for client status, score, and progress based on client
    // ID
    private Map<String, JLabel> clientStatusLabels;
    private Map<String, JLabel> clientScoreLabels;
    private Map<String, JLabel> clientProgressLabels;

    // Panel to display client information
    private JPanel clientPanel;

    // Constructor to initialize the server monitor GUI
    public QuizServerGUI(QuizServer server) {
        // Set window title, size, and default close operation
        setTitle("Quiz Server Monitor");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize the maps to hold client status, score, and progress labels
        clientStatusLabels = new HashMap<>();
        clientScoreLabels = new HashMap<>();
        clientProgressLabels = new HashMap<>();

        // Initialize the text area to display server messages
        statusArea = new JTextArea();
        statusArea.setEditable(false); // Make the text area non-editable
        add(new JScrollPane(statusArea), BorderLayout.CENTER); // Add text area to the center of the window

        // Create a panel for displaying client statuses, scores, and progress
        clientPanel = new JPanel();
        clientPanel.setLayout(new GridLayout(0, 4, 10, 10)); // 4 columns: Client ID, Status, Score, Progress
        add(new JScrollPane(clientPanel), BorderLayout.SOUTH); // Add panel to the bottom of the window
    }

    // Method to add a new client to the GUI (with client ID, status, score, and
    // progress labels)
    public void addClient(String clientId, int totalQuestions) {
        if (clientStatusLabels.containsKey(clientId)) {
            // If client already exists, no need to add again
            return;
        }

        // Create labels for client status, score, and progress
        JLabel idLabel = new JLabel(clientId);
        JLabel statusLabel = new JLabel("Status: Connected");
        JLabel scoreLabel = new JLabel("Score: 0");
        JLabel progressLabel = new JLabel("Progress: 0/" + totalQuestions);

        // Store the labels in their respective maps, using client ID as the key
        clientStatusLabels.put(clientId, statusLabel);
        clientScoreLabels.put(clientId, scoreLabel);
        clientProgressLabels.put(clientId, progressLabel);

        // Add the client ID, status label, score label, and progress label to the panel
        clientPanel.add(idLabel);
        clientPanel.add(statusLabel);
        clientPanel.add(scoreLabel);
        clientPanel.add(progressLabel);

        // Refresh the layout to display the new client
        clientPanel.revalidate();
        clientPanel.repaint();
    }

    // Method to update the status of a client in the GUI
    public void updateClientStatus(String clientId, String status) {
        SwingUtilities.invokeLater(() -> {
            JLabel statusLabel = clientStatusLabels.get(clientId);
            if (statusLabel != null) {
                statusLabel.setText("Status: " + status);
            }
        });
    }

    // Method to update the score of a client in the GUI
    public void updateClientScore(String clientId, int score) {
        SwingUtilities.invokeLater(() -> {
            JLabel scoreLabel = clientScoreLabels.get(clientId);
            if (scoreLabel != null) {
                scoreLabel.setText("Score: " + score);
            }
        });
    }

    // Method to update the progress of a client in the GUI
    public void updateClientProgress(String clientId, int currentQuestion, int totalQuestions) {
        SwingUtilities.invokeLater(() -> {
            JLabel progressLabel = clientProgressLabels.get(clientId);
            if (progressLabel != null) {
                progressLabel.setText("Progress: " + currentQuestion + "/" + totalQuestions);
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
}