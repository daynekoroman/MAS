package com.rida.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * Created by unrealwork on 09.04.16.
 */
public class FileUtils {
    public static int[][] readPairArray(URL url) {
        return null;
    }

    public static int[][] readSquareIntegerMatrix(URL fileUrl) throws IOException {
        int n;
        int[][] matrix;

        File file;
        if (fileUrl != null) {
            file = new File(fileUrl.getFile());
        } else {
            file = null;
        }

        BufferedReader fin = null;
        if (file != null) {
            try {
                fin = new BufferedReader(new FileReader(file));
                String[] size = fin.readLine().split(" ");
                n = Integer.parseInt(size[0]);
                matrix = new int[n][n];
                for (int i = 0; i < n; i++) {
                    String[] matr = fin.readLine().split(" ");
                    for (int j = 0; j < matr.length; j++) {
                        matrix[i][j] = Integer.parseInt(matr[j]);
                    }
                }
                return matrix;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fin != null) {
                    fin.close();
                }
            }
        }
        return null;
    }
}
