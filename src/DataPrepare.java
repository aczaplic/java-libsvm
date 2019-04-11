import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class DataPrepare{

    static double[][] readData(String pathToFile) throws FileNotFoundException {
        ArrayList<ArrayList<String>> result = new ArrayList<>();

        try (Scanner s = new Scanner(new FileReader(pathToFile))) {
            s.nextLine();
            while (s.hasNextLine()) {
                ArrayList<String> record = new ArrayList<>();
                String[] line = s.nextLine().trim().split("\\t");
                Collections.addAll(record, line);
                result.add(record);
            }
        }
        Collections.shuffle(result);

        double data[][] = new double [result.size()][];
        for (int i = 0; i < result.size(); i++) {
            ArrayList<String> row = result.get(i);
            data[i] = new double [row.size()-1];
            for (int j=1; j < row.size(); j++) {
                data[i][j-1] = Double.parseDouble(row.get(j));
            }
        }

        return data;
    }

    static double[][] getMinMax(double[][] data){
        double[][] min_max = new double[2][data.length];
        min_max[0] = new double[data.length];
        min_max[1] = new double[data.length];
        for (int i = 0; i < data.length; i++){
            min_max[0][i] = 0;
            min_max[1][i] = 0;
            for (int j = 1; j < data[0].length; j++){
                if(data[i][j] > min_max[1][i])
                    min_max[1][i] = data[i][j];
                else if(data[i][j] < min_max[0][i])
                    min_max[0][i] = data[i][j];
            }
        }
        return min_max;
    }

    static double[][] normalizeMinMax(double[][] data, double[][] min_max) {
        for (int i = 0; i < data.length; i++){
            for (int j = 1; j < data[0].length; j++){
                data[i][j] = (data[i][j] - min_max[0][i]) / (min_max[1][i] - min_max[0][i]);
            }
        }
        return data;
    }
}
