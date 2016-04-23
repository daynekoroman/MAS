package com.rida.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by shmagrinsky on 10.04.16.
 */
public class SubsetGenerator<T> {
    private void getSubsets(List<T> superSet, int k, int idx, Set<T> current, List<Set<T>> solution) {
        //successful stop clause
        if (current.size() == k) {
            solution.add(new HashSet<>(current));
            return;
        }
        //unseccessful stop clause
        if (idx == superSet.size()) return;
        T x = superSet.get(idx);
        current.add(x);
        //"guess" x is in the subset
        getSubsets(superSet, k, idx+1, current, solution);
        current.remove(x);
        //"guess" x is not in the subset
        getSubsets(superSet, k, idx+1, current, solution);
    }

    public List<Set<T>> getSubsets(List<T> superSet, int k) {
        List<Set<T>> res = new ArrayList<>();
        getSubsets(superSet, k, 0, new HashSet<T>(), res);
        return res;
    }

    public Set<Set<T>> powerSet(Set<T> set , int limitSize) {
        Set<Set<T>> resultSet = new HashSet<>();
        List<T> list = new ArrayList<>();
        for (T item: set) {
            list.add(item);
        }
        for (int i = 1; i<=limitSize; i++) {
            for (Set<T> listItem: getSubsets(list, i)) {
                resultSet.add(listItem);
            }
        }
        return resultSet;
    }
}
