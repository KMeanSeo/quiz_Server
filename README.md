# Quiz Game Application Report (Computer Network HW1)

**Author:** KimMinseo

---

## Overview
This project develops a **quiz game application** using **Java sockets**. The application enables clients to connect to a server and play a text-based quiz game. 

---

## Objectives
- Develop a client-server quiz game using Java sockets.
- Define an **application-layer protocol**.
- Ensure the client can connect to the server, receive questions, and provide responses.

---

## Requirements

### Server
1. **Store questions and answers** in arrays or lists.
2. **Send questions** to clients and evaluate responses.
3. Provide feedback for client answers:
   - "Correct Answer"
   - "Wrong Answer"
4. **Calculate and send the final score** after all questions are answered.

### Client
1. Connect to the server using IP and port information.
2. Receive questions and submit answers.
3. Display feedback and final scores.

---

## Features

### Server
- **Question Storage:** Questions loaded from `quiz_list.csv`.
- **Client Communication:** Uses `ServerSocket` for connections and `ThreadPool` for handling multiple clients.
- **Protocols:** Handles requests such as `CONNECT|SERVER`, `QUIZ|REQUEST`, and `ANSWER`.

### Client
- **Configuration File:** Reads server info from `server_info.dat` or defaults to `localhost:1234`.
- **Quiz Interaction:** Requests questions, submits answers, and receives feedback.

---

## Protocols
- **Message Format:** ASCII-based using `|` as a delimiter.
- Example:
  - Request: `QUIZ|REQUEST`
  - Response: `301|Quiz_Content|<Question>|<Current_Question>/<Total_Questions>`

---

## Technologies
- **Programming Language:** Java
- **Network Protocol:** TCP
- **Message Format:** ASCII

---

## GUI Features
- **Server GUI:** Displays connected clients, progress, and logs.
- **Client GUI:** Provides an interface for submitting answers and viewing feedback.

---

## Output Examples

### Server Logs
- Monitors requests and responses.
- Displays client progress and connection status.

### Client Interface
- Displays quiz questions and allows answer submissions.
- Provides feedback such as "Correct Answer" or "Wrong Answer."

---

##How to Run

### Run Server
1. Compile the server file:
   ```
   javac QuizServer.java
   ```
2. Run the server:
   ```
   java QuizServer
   ```

### Run Client
1. Compile the client GUI file:
   ```
   javac QuizClientGUI.java
   ```
2. Run the client:
   ```
   java QuizClientGUI
   ```

---

## Output Examples

### Server Logs
- Monitors requests and responses.
- Displays client progress and connection status.

### Client Interface
- Displays quiz questions and allows answer submissions.
- Provides feedback such as "Correct Answer" or "Wrong Answer."


---


## Final Notes
The application successfully supports multiple clients, real-time feedback, and a clean GUI for both server and client interactions.

---
