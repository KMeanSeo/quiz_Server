import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

public class QuizClientGUI extends JFrame {
    // GUI components
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton submitButton;
    private JLabel questionNumberLabel;

    // Instance of the QuizClient class to handle server communication
    private QuizClient quizClient;

    // Variables to track current and total questions
    private int currentQuestionNumber = 0;
    private int totalQuestions = 0;

    // Constructor for setting up the GUI and initializing the connection
    public QuizClientGUI() {
        // Set window title, size, and default close operation
        setTitle("Quiz Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create and configure chat area to display server messages
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Create panel for user input
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        // Create text field for entering answers
        inputField = new JTextField();
        inputPanel.add(inputField, BorderLayout.CENTER);

        // Create button to submit answer
        submitButton = new JButton("Submit Answer");
        submitButton.addActionListener(new SubmitAnswerListener());
        inputPanel.add(submitButton, BorderLayout.EAST);

        // Label to show the current question number
        questionNumberLabel = new JLabel("Question: 0/0");
        inputPanel.add(questionNumberLabel, BorderLayout.NORTH);

        // Add input panel to the bottom of the frame
        add(inputPanel, BorderLayout.SOUTH);

        try {
            // Establish connection to the server
            quizClient = new QuizClient();
            String connectResponse = quizClient.connectToServer();
            chatArea.append("Server: " + connectResponse + "\n");

            // If connection is accepted, enable input and request the first quiz
            if (connectResponse.startsWith("200|Connection_Accepted")) {
                inputField.setEnabled(true);
                submitButton.setEnabled(true);
                totalQuestions = Integer.parseInt(connectResponse.split("\\|")[2]);
                requestQuiz();
            } else {
                // If connection is not accepted, disable input and submit button
                inputField.setEnabled(false);
                submitButton.setEnabled(false);
            }
        } catch (IOException e) {
            // Show error message if connection fails
            showError("Failed to connect to the server.");
            inputField.setEnabled(false);
            submitButton.setEnabled(false);
        }
    }

    // Method to request a quiz question from the server
    private void requestQuiz() {
        try {
            // Request quiz content from the server
            String response = quizClient.requestQuiz();
            processResponse(response);
        } catch (IOException e) {
            // Show error if quiz request fails
            showError("Failed to request quiz from the server.");
        }
    }

    // Method to send the user's answer to the server
    private void sendAnswer() {
        String answer = inputField.getText();

        // Check if answer is empty
        if (answer.isEmpty()) {
            showError("Answer cannot be empty.");
            return;
        }

        try {
            // Send the user's answer to the server
            String response = quizClient.sendAnswer(answer);
            processResponse(response);
        } catch (IOException e) {
            // Show error if sending answer fails
            showError("Failed to send answer to the server.");
        }

        // Clear input field after sending answer
        inputField.setText("");
    }

    // Method to process the server's response
    protected void processResponse(String response) {
        if (response.startsWith("201|Quiz_Content")) {
            // If the server sends a new quiz question, update UI with the question
            String[] parts = response.split("\\|");
            String question = parts[2];
            currentQuestionNumber = Integer.parseInt(parts[3].split("/")[0]);
            chatArea.append("Question: " + question + "\n");
            questionNumberLabel.setText("Question: " + currentQuestionNumber + "/" + totalQuestions);
        } else if (response.startsWith("202|Correct_Answer")) {
            // If the answer is correct, display a message and request the next quiz
            chatArea.append("Server: Correct Answer!\n\n");
            requestQuiz();
        } else if (response.startsWith("203|Wrong_Answer")) {
            // If the answer is wrong, display a message and request the next quiz
            chatArea.append("Server: Wrong Answer.\n\n");
            requestQuiz();
        } else if (response.startsWith("204|Final_Score")) {
            // If the quiz is finished, show the final score and exit prompt
            chatArea.append("Server: Quiz finished. Final Score: " + response.split("\\|")[2] + "\n\n");
            submitButton.setEnabled(false);
            inputField.setEnabled(false);

            // Show final score and ask if the user wants to exit
            String score = response.split("\\|")[2];
            JOptionPane.showMessageDialog(this, "Your final score is: " + score, "Final Score",
                    JOptionPane.INFORMATION_MESSAGE);

            int result = JOptionPane.showConfirmDialog(this,
                    "Do you want to disconnect from the server and close the program?", "Exit Confirmation",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    quizClient.closeConnection(); // Disconnect from the server
                } catch (IOException e) {
                    showError("Failed to disconnect: " + e.getMessage());
                }
                System.exit(0); // Exit the program
            }
        } else {
            // If response is unrecognized, just display it in chat area
            chatArea.append("Server: " + response + "\n");
        }
    }

    // Method to display error messages in a dialog box
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Listener for the "Submit Answer" button
    private class SubmitAnswerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sendAnswer();
        }
    }

    // Main method to launch the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizClientGUI clientGUI = new QuizClientGUI();
            clientGUI.setVisible(true);
        });
    }
}