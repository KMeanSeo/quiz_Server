import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class QuizClientGUI extends JFrame {
    private QuizClient quizClient;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton submitButton;
    private JLabel serverStatusLabel;

    public QuizClientGUI() {
        try {
            quizClient = new QuizClient();
            initializeGUI();
            connectToServer();
            requestQuiz();
        } catch (IOException e) {
            showError("Error connecting to server.");
            serverStatusLabel.setText("Server Status: Disconnected");
        }
    }

    private void initializeGUI() {
        setTitle("GPT-like Quiz Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 채팅 영역
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        // 입력 필드 및 버튼 설정
        inputField = new JTextField(30);
        submitButton = new JButton("Send");
        serverStatusLabel = new JLabel("Server Status: Disconnected", SwingConstants.RIGHT);

        // 패널 설정
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(submitButton, BorderLayout.EAST);

        // 레이아웃 구성
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        chatPanel.add(serverStatusLabel, BorderLayout.PAGE_END);
        add(chatPanel);

        submitButton.addActionListener(new SubmitAnswerListener());
        inputField.addActionListener(new SubmitAnswerListener());

        // 텍스트 필드 상태 변경 감지
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                toggleSubmitButton();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                toggleSubmitButton();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                toggleSubmitButton();
            }
            private void toggleSubmitButton() {
                submitButton.setEnabled(!inputField.getText().trim().isEmpty());
            }
        });
    }

    private void connectToServer() {
        try {
            String response = quizClient.connectToServer();
            if (response != null && response.startsWith("200|Connection_Accepted")) {
                serverStatusLabel.setText("Server Status: Connected");
                chatArea.append("Connected to server.\n\n");
                inputField.setEnabled(true);
            } else {
                serverStatusLabel.setText("Server Status: Connection Failed");
            }
        } catch (IOException e) {
            showError("Error during connection.");
        }
    }

    private void requestQuiz() {
        try {
            String quizResponse = quizClient.requestQuiz();
            if (quizResponse != null && quizResponse.startsWith("201|Quiz_Content")) {
                String[] parts = quizResponse.split("\\|", 4);
                if (parts.length >= 4) {
                    chatArea.append("GPT: " + parts[2] + "\nProgress: (" + parts[3] + ")\n\n");
                    inputField.setEnabled(true);
                    inputField.requestFocus();
                } else {
                    chatArea.append("GPT: Error: Incomplete question response.\n");
                }
            }
        } catch (IOException e) {
            showError("Error requesting quiz question.");
        }
    }

    private void sendAnswer() {
        String userAnswer = inputField.getText().trim();
        if (!userAnswer.isEmpty()) {
            chatArea.append("You: " + userAnswer + "\n");
            try {
                String answerResponse = quizClient.sendAnswer(userAnswer);
                if (answerResponse != null) {
                    if (answerResponse.startsWith("202|Correct_Answer")) {
                        chatArea.append("GPT: Correct Answer!\n\n");
                    } else if (answerResponse.startsWith("203|Wrong_Answer")) {
                        chatArea.append("GPT: Wrong Answer.\n\n");
                    } else if (answerResponse.startsWith("204|Final_Score")) {
                        chatArea.append("GPT: Quiz finished. Final Score: " + answerResponse.split("\\|")[2] + "\n\n");
                        submitButton.setEnabled(false);
                        inputField.setEnabled(false);
                    }
                }
                inputField.setText("");
                requestQuiz();
            } catch (IOException e) {
                showError("Error sending answer.");
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
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
