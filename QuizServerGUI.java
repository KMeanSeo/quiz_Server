import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class QuizServerGUI extends JFrame {
    private JTextArea statusArea;
    private JPanel clientPanel;
    private Map<String, JLabel> clientStatusLabels;
    private Map<String, JLabel> clientScoreLabels;
    private Map<String, JLabel> clientProgressLabels;

    public QuizServerGUI(QuizServer server) {
        setTitle("Quiz Server");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

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

        // Make the GUI visible
        setVisible(true);
    }

    // Method to add a new client to the GUI (with client ID, status, score, and
    // progress labels)
    public void addClient(String clientId, int totalQuestions) {
        JLabel statusLabel = new JLabel("Connected");
        JLabel scoreLabel = new JLabel("0");
        JLabel progressLabel = new JLabel("0/" + totalQuestions);

        clientStatusLabels.put(clientId, statusLabel);
        clientScoreLabels.put(clientId, scoreLabel);
        clientProgressLabels.put(clientId, progressLabel);

        clientPanel.add(new JLabel(clientId));
        clientPanel.add(statusLabel);
        clientPanel.add(scoreLabel);
        clientPanel.add(progressLabel);

        // Refresh the client panel to show the new client
        clientPanel.revalidate();
        clientPanel.repaint();
    }

    // Method to update client status in the GUI
    public void updateClientStatus(String clientId, String status) {
        JLabel statusLabel = clientStatusLabels.get(clientId);
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    // Method to update client score in the GUI
    public void updateClientScore(String clientId, int score) {
        JLabel scoreLabel = clientScoreLabels.get(clientId);
        if (scoreLabel != null) {
            scoreLabel.setText(String.valueOf(score));
        }
    }

    // Method to update client progress in the GUI
    public void updateClientProgress(String clientId, int currentQuestion, int totalQuestions) {
        JLabel progressLabel = clientProgressLabels.get(clientId);
        if (progressLabel != null) {
            progressLabel.setText(currentQuestion + "/" + totalQuestions);
        }
    }

    // Method to append a message to the status area
    public void appendStatusMessage(String message) {
        statusArea.append(message + "\n");
    }

    // Main method to test the GUI independently
    public static void main(String[] args) {
        // Create a dummy QuizServer instance (null for testing purposes)
        QuizServer dummyServer = null;

        // Create and display the GUI
        SwingUtilities.invokeLater(() -> new QuizServerGUI(dummyServer));
    }
}