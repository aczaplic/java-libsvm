package dataset;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class FileHandler {

    /**
     * Reads file line after line (separated by tabulator) to ArrayList
     *
     * @return ArrayList with file content
     */
    static ArrayList readFile(String pathToFile) throws FileNotFoundException {
        ArrayList<ArrayList<String>> result = new ArrayList<>();

        try (Scanner s = new Scanner(new java.io.FileReader(pathToFile))) {
            s.nextLine();
            while (s.hasNextLine()) {
                ArrayList<String> record = new ArrayList<>();
                String[] line = s.nextLine().trim().split("\\t");
                Collections.addAll(record, line);
                result.add(record);
            }
        }
        //Collections.shuffle(result);
        return result;
    }
}
