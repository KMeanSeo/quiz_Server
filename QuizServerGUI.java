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
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30));

        clientStatusLabels = new HashMap<>();
        clientScoreLabels = new HashMap<>();
        clientProgressLabels = new HashMap<>();

        // Status area with custom styling
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        statusArea.setBackground(new Color(40, 40, 40));
        statusArea.setForeground(Color.WHITE);
        statusArea.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Server Logs", 0, 0, new Font("Arial", Font.BOLD, 14), Color.LIGHT_GRAY));

        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        statusScrollPane.setPreferredSize(new Dimension(400, 200));
        add(statusScrollPane, BorderLayout.NORTH);

        // Client panel with custom layout
        clientPanel = new JPanel(new GridBagLayout());
        clientPanel.setBackground(new Color(30, 30, 30));
        clientPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Client Status", 0, 0, new Font("Arial", Font.BOLD, 14), Color.LIGHT_GRAY));

        JScrollPane clientScrollPane = new JScrollPane(clientPanel);
        clientScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(clientScrollPane, BorderLayout.CENTER);

        // Footer with server info
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(new Color(40, 40, 40));
        JLabel footerLabel = new JLabel("Quiz Server © 2024");
        footerLabel.setForeground(Color.LIGHT_GRAY);
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        footerPanel.add(footerLabel);
        add(footerPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    public void addClient(String clientId, int totalQuestions) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel idLabel = new JLabel("Client: " + clientId);
        idLabel.setForeground(Color.CYAN);
        idLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = clientStatusLabels.size();
        clientPanel.add(idLabel, gbc);

        JLabel statusLabel = new JLabel("Connected");
        statusLabel.setForeground(Color.GREEN);
        clientStatusLabels.put(clientId, statusLabel);
        gbc.gridx = 1;
        clientPanel.add(statusLabel, gbc);

        JLabel scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        clientScoreLabels.put(clientId, scoreLabel);
        gbc.gridx = 2;
        clientPanel.add(scoreLabel, gbc);

        JLabel progressLabel = new JLabel("Progress: 0/" + totalQuestions);
        progressLabel.setForeground(Color.ORANGE);
        clientProgressLabels.put(clientId, progressLabel);
        gbc.gridx = 3;
        clientPanel.add(progressLabel, gbc);

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