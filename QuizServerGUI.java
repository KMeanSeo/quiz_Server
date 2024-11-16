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
    private Map<String, JTextArea> clientLogs;

    public QuizServerGUI(QuizServer server) {
        setTitle("Quiz Server");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize maps
        clientStatusLabels = new HashMap<>();
        clientScoreLabels = new HashMap<>();
        clientProgressLabels = new HashMap<>();
        clientLogs = new HashMap<>();

        // Status area (Top)
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        statusArea.setBackground(new Color(45, 45, 45));
        statusArea.setForeground(Color.LIGHT_GRAY);
        statusArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                "Server Logs",
                0,
                0,
                new Font("Arial", Font.BOLD, 14),
                Color.LIGHT_GRAY));
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        statusScrollPane.setPreferredSize(new Dimension(400, 200));
        add(statusScrollPane, BorderLayout.NORTH);

        // Client panel (Center)
        clientPanel = new JPanel(new GridBagLayout());
        clientPanel.setBackground(new Color(35, 35, 35));
        clientPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                "Client Status",
                0,
                0,
                new Font("Arial", Font.BOLD, 14),
                Color.LIGHT_GRAY));
        JScrollPane clientScrollPane = new JScrollPane(clientPanel);
        clientScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(clientScrollPane, BorderLayout.CENTER);

        // Footer (Bottom)
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(new Color(40, 40, 40));
        JLabel footerLabel = new JLabel("Quiz Server © 2024");
        footerLabel.setForeground(Color.LIGHT_GRAY);
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        footerPanel.add(footerLabel);
        add(footerPanel, BorderLayout.SOUTH);

        // Set visibility
        setVisible(true);
    }

    public void addClient(String clientId, int totalQuestions) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Client ID
        JLabel idLabel = new JLabel("Client: " + clientId);
        idLabel.setForeground(new Color(0, 191, 255)); // Cyan
        idLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = clientStatusLabels.size();
        clientPanel.add(idLabel, gbc);

        // Client Status
        JLabel statusLabel = new JLabel("Connected");
        statusLabel.setForeground(Color.GREEN);
        clientStatusLabels.put(clientId, statusLabel);
        gbc.gridx = 1;
        clientPanel.add(statusLabel, gbc);

        // Client Score
        JLabel scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.LIGHT_GRAY);
        clientScoreLabels.put(clientId, scoreLabel);
        gbc.gridx = 2;
        clientPanel.add(scoreLabel, gbc);

        // Client Progress
        JLabel progressLabel = new JLabel("Progress: 0/" + totalQuestions);
        progressLabel.setForeground(new Color(255, 165, 0)); // Orange
        clientProgressLabels.put(clientId, progressLabel);
        gbc.gridx = 3;
        clientPanel.add(progressLabel, gbc);

        // Client-specific log area
        JTextArea clientLogArea = new JTextArea(5, 30);
        clientLogArea.setEditable(false);
        clientLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        clientLogArea.setBackground(new Color(40, 40, 40));
        clientLogArea.setForeground(Color.LIGHT_GRAY);
        clientLogArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                "Logs for " + clientId,
                0,
                0,
                new Font("Arial", Font.BOLD, 12),
                Color.LIGHT_GRAY));
        clientLogs.put(clientId, clientLogArea);

        gbc.gridx = 4;
        clientPanel.add(new JScrollPane(clientLogArea), gbc);

        refreshClientPanel();
    }

    public void updateClientStatus(String clientId, String status) {
        SwingUtilities.invokeLater(() -> {
            JLabel statusLabel = clientStatusLabels.get(clientId);
            if (statusLabel != null) {
                statusLabel.setText(status);
                statusLabel.setForeground(status.equalsIgnoreCase("Connected") ? Color.GREEN : Color.RED);
            }
        });
    }

    public void updateClientScore(String clientId, int score) {
        SwingUtilities.invokeLater(() -> {
            JLabel scoreLabel = clientScoreLabels.get(clientId);
            if (scoreLabel != null) {
                scoreLabel.setText("Score: " + score);
            }
        });
    }

    public void updateClientProgress(String clientId, int currentQuestion, int totalQuestions) {
        SwingUtilities.invokeLater(() -> {
            JLabel progressLabel = clientProgressLabels.get(clientId);
            if (progressLabel != null) {
                progressLabel.setText("Progress: " + currentQuestion + "/" + totalQuestions);
            }
        });
    }

    public void logClientMessage(String clientId, String message) {
        SwingUtilities.invokeLater(() -> {
            JTextArea clientLogArea = clientLogs.get(clientId);
            if (clientLogArea != null) {
                clientLogArea.append(message + "\n");
                clientLogArea.setCaretPosition(clientLogArea.getDocument().getLength());
            }
        });
    }

    public void appendStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(message + "\n");
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }

    public void refreshClientPanel() {
        SwingUtilities.invokeLater(() -> {
            clientPanel.revalidate();
            clientPanel.repaint();
        });
    }
}