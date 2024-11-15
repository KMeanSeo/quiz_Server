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

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        add(new JScrollPane(statusArea), BorderLayout.CENTER);

        clientPanel = new JPanel();
        clientPanel.setLayout(new GridLayout(0, 4, 10, 10));
        add(new JScrollPane(clientPanel), BorderLayout.SOUTH);

        setVisible(true);
    }

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

        refreshClientPanel();
    }

    public void updateClientStatus(String clientId, String status) {
        SwingUtilities.invokeLater(() -> {
            JLabel statusLabel = clientStatusLabels.get(clientId);
            if (statusLabel != null)
                statusLabel.setText(status);
        });
    }

    public void updateClientScore(String clientId, int score) {
        SwingUtilities.invokeLater(() -> {
            JLabel scoreLabel = clientScoreLabels.get(clientId);
            if (scoreLabel != null)
                scoreLabel.setText(String.valueOf(score));
        });
    }

    public void updateClientProgress(String clientId, int currentQuestion, int totalQuestions) {
        SwingUtilities.invokeLater(() -> {
            JLabel progressLabel = clientProgressLabels.get(clientId);
            if (progressLabel != null)
                progressLabel.setText(currentQuestion + "/" + totalQuestions);
        });
    }

    public void refreshClientPanel() {
        SwingUtilities.invokeLater(() -> {
            clientPanel.revalidate();
            clientPanel.repaint();
        });
    }

    public void appendStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }
}