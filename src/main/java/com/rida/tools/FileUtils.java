package com.rida.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Утилиты для чтения различных структур данных из файла
 */
public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);


    private FileUtils() {

    }

    /**
     * Считываем целочисленную матрицу из файла
     *
     * @param fileUrl -  url файла
     * @return двумерный массив, модержащий матрицу
     * @throws IOException
     */
    public static int[][] readSquareIntegerMatrix(URL fileUrl) throws IOException {
        int[][] matrix;
        List<String> lines = readLines(fileUrl);
        int i = 0;
        matrix = new int[lines.size()][];
        for (String line : lines) {
            matrix[i] = parseIntArray(line);
            i++;
        }
        return matrix;
    }

    private static List<String> readLines(URL fileUrl) {
        List<String> lines = null;
        BufferedReader bufferedReader;
        try {
            File file = new File(fileUrl.getFile());
            FileReader fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            lines = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            fileReader.close();
        } catch (IOException e) {
            LOG.error("Failed to read data from file caused : ", e);
        }
        return lines;
    }

    private static int[] parseIntArray(String line) {
        String[] tokenArray = line.trim().split(" ");
        int[] numArray = new int[tokenArray.length];
        int i = 0;
        for (String token : tokenArray) {
            numArray[i] = Integer.parseInt(token);
            i++;
        }
        return numArray;
    }
}
