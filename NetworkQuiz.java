import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetworkQuiz {

    public static List<String[]> loadQuiz(String filePath) {
        List<String[]> quizList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header line
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", 2); // Split into question and answer
                quizList.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return quizList;
    }
}
