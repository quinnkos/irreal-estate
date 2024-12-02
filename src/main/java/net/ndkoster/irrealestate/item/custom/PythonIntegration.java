package net.ndkoster.irrealestate.item.custom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class PythonIntegration {

    public int callPricer(int sqftOfHouse, int bedrooms, boolean isFurnished) throws IOException {

        String[] args = {
                Integer.toString(sqftOfHouse),
                Integer.toString(bedrooms),
                Boolean.toString(isFurnished)
        };

        ProcessBuilder pb = new ProcessBuilder("python3",
                "price-prediction/pricer.py", args[0], args[1], args[2]);
        pb.redirectErrorStream(true);
        File workingDirectory = new File("../src/main/python");
        pb.directory(workingDirectory);
        Process p = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        try {

            return (int)(Double.parseDouble(reader.readLine()));

        } catch (NumberFormatException e) {

            String errorLine;
            while ((errorLine = reader.readLine()) != null) {
                System.out.println(errorLine);
            }
            return -1;

        }

    }
}
