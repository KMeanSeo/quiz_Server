import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

public class QuizClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton submitButton;
    private JLabel questionNumberLabel;
    private JProgressBar progressBar;
    private QuizClient quizClient;
    private int currentQuestionNumber = 0;
    private int totalQuestions = 0;

    public QuizClientGUI() {
        setTitle("Quiz Client");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(40, 40, 40));
        add(mainPanel);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(new Color(220, 220, 220));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237)),
                "Quiz Messages",
                0,
                0,
                new Font("Arial", Font.BOLD, 16),
                new Color(173, 216, 230)));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBackground(new Color(50, 50, 50));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180)),
                "Answer Input",
                0,
                0,
                new Font("Arial", Font.BOLD, 14),
                Color.WHITE));

        // Input field
        inputField = new JTextField();
        inputField.setFont(new Font("Consolas", Font.PLAIN, 16));
        inputField.setBackground(new Color(40, 40, 40));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(new Color(135, 206, 250));
        inputField.setEnabled(false);
        inputPanel.add(inputField, BorderLayout.CENTER);

        // Submit button
        submitButton = new JButton("Submit");
        submitButton.setFont(new Font("Arial", Font.BOLD, 14));
        submitButton.setBackground(new Color(70, 130, 180));
        submitButton.setForeground(Color.BLACK);
        submitButton.setEnabled(false);
        submitButton.setFocusPainted(false); // Disable focus outline for better appearance
        submitButton.addActionListener(new SubmitAnswerListener());
        inputPanel.add(submitButton, BorderLayout.EAST);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(50, 50, 50));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Question number label
        questionNumberLabel = new JLabel("Question: 0/0");
        questionNumberLabel.setFont(new Font("Arial", Font.BOLD, 16));
        questionNumberLabel.setForeground(new Color(135, 206, 250));
        headerPanel.add(questionNumberLabel, BorderLayout.WEST);

        // Progress bar
        progressBar = new JProgressBar(0, 10);
        progressBar.setStringPainted(false);
        progressBar.setForeground(new Color(50, 205, 50));
        progressBar.setBackground(new Color(30, 30, 30));
        headerPanel.add(progressBar, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        connectToServer();
    }

    private void connectToServer() {
        try {
            quizClient = new QuizClient();
            String connectResponse = quizClient.connectToServer();

            if (connectResponse.startsWith("200|Connection_Accepted")) {
                totalQuestions = Integer.parseInt(connectResponse.split("\\|")[2]);
                progressBar.setMaximum(totalQuestions);

                SwingUtilities.invokeLater(() -> {
                    int response = JOptionPane.showConfirmDialog(
                            this,
                            "서버에 연결 성공했습니다. 퀴즈를 시작하시겠습니까?",
                            "서버 연결 성공",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);

                    if (response == JOptionPane.YES_OPTION) {
                        startQuiz();
                    } else {
                        System.exit(0);
                    }
                });
            } else {
                inputField.setEnabled(false);
                submitButton.setEnabled(false);
            }
        } catch (IOException e) {
            showError("Failed to connect to the server.", true);
            inputField.setEnabled(false);
            submitButton.setEnabled(false);
        }
    }

    private void startQuiz() {
        inputField.setEnabled(true);
        submitButton.setEnabled(true);
        requestQuiz();
    }

    private void requestQuiz() {
        try {
            String response = quizClient.requestQuiz();
            processResponse(response);
        } catch (IOException e) {
            showError("Failed to request quiz from the server.", false);
        }
    }

    private void sendAnswer() {
        String answer = inputField.getText();

        if (answer.isEmpty()) {
            showError("Answer cannot be empty.", false);
            return;
        }

        chatArea.append("You: " + answer + "\n");

        try {
            String response = quizClient.sendAnswer(answer);
            processResponse(response);
        } catch (IOException e) {
            showError("Failed to send answer to the server.", false);
        }

        inputField.setText("");
    }

    protected void processResponse(String response) {
        if (response == null || response.isEmpty()) {
            chatArea.append("Server: No response received.\n");
            return;
        }

        if (response.startsWith("201|Quiz_Content")) {
            String[] parts = response.split("\\|");
            String question = parts[2];
            currentQuestionNumber = Integer.parseInt(parts[3].split("/")[0]);
            chatArea.append("Question: " + question + "\n");
            questionNumberLabel.setText("Question: " + currentQuestionNumber + "/" + totalQuestions);
            progressBar.setValue(currentQuestionNumber);

        } else if (response.startsWith("202|Correct_Answer")) {
            chatArea.append("Server: Correct Answer!\n\n");
            requestQuiz();

        } else if (response.startsWith("203|Wrong_Answer")) {
            chatArea.append("Server: Wrong Answer.\n\n");
            requestQuiz();

        } else if (response.startsWith("204|Final_Score")) {
            chatArea.append("Server: Quiz finished. Final Score: " + response.split("\\|")[2] + "\n\n");
            submitButton.setEnabled(false);
            inputField.setEnabled(false);
            progressBar.setValue(totalQuestions);
            int result = JOptionPane.showConfirmDialog(this,
                    "Your final score is: " + response.split("\\|")[2] + "\nDo you want to exit?", "Final Score",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                System.exit(0);
            }
        } else {
            chatArea.append("Server: " + response + "\n");
        }
    }

    private void showError(String message, boolean exitOnClose) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        if (exitOnClose) {
            System.exit(0);
        }
    }

    private class SubmitAnswerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendAnswer();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizClientGUI clientGUI = new QuizClientGUI();
            clientGUI.setVisible(true);
        });
    }
}