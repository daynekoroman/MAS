package com.rida.tools;

import java.util.HashSet;
import java.util.Random;

/**
 * Created by unrealwork on 04.05.16.
 */
public class GraphGenerator {

    public static void drivers() {
        HashSet<Integer> set = new HashSet<>();
        Random rand = new Random();
        while (set.size() < 40)
            set.add(rand.nextInt(60));

        for (Integer n : set)
            System.out.println(n + " " + (rand.nextInt(15) + 60));

    }

    public static void main(String[] args) {

        drivers();

//
//
//
//        int [][] arr = new int [75][75];
//        for(int i = 0; i < 40; i ++){
//            arr[i][i / 2 + 40] = 1;
//            arr[i / 2 + 40][i] = 1;
//            if (i != 39)
//            {
//                arr[i][i +1] = 1;
//                arr[i + 1][i] = 1;
//            }
//        }
//        arr[39][0] = 1;
//        arr[0][39] = 1;
//        for(int i = 40; i < 60; i ++){
//            arr[i][(i - 40) / 2 + 60] = 1;
//            arr[(i - 40) / 2 + 60][i] = 1;
//            if (i != 59)
//            {
//                arr[i][i +1] = 1;
//                arr[i + 1][i] = 1;
//            }
//        }
//        arr[59][40] = 1;
//        arr[40][59] = 1;
//        for(int i = 60; i < 70; i ++){
//            arr[i][(i - 60) / 2 + 70] = 1;
//            arr[(i - 60) / 2 + 70][i] = 1;
//            if (i != 69)
//            {
//                arr[i][i +1] = 1;
//                arr[i + 1][i] = 1;
//            }
//        }
//        arr[69][60] = 1;
//        arr[60][69] = 1;
//        for(int i = 70; i < 74; i ++){
//            for (int j = i + 1; j < 75; j ++){
//                arr[i][j] = 1;
//                arr[j][i] = 1;
//            }
//        }
//
//
//        for (int i = 0; i < 75; i ++){
//            for (int j = 0; j < 75; j ++){
//                System.out.print(arr[i][j] + " ");
//            }
//            System.out.println();
//        }
    }
}
