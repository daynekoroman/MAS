package com.rida.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Класс для генерации подмножеств множества объектов
 *
 * @param <T> - класс Объекта содержащийся в исходном множестве
 */
public class SubsetGenerator<T> {
    private void generateSubset(List<T> superSet, int k, int idx, Set<T> current, List<Set<T>> solution) {
        //successful stop clause
        if (current.size() == k) {
            solution.add(new HashSet<>(current));
            return;
        }
        //unseccessful stop clause
        int setSize = superSet.size();
        if (idx == setSize) {
            return;
        }
        T x = superSet.get(idx);
        current.add(x);
        //"guess" x is in the subset
        generateSubset(superSet, k, idx + 1, current, solution);
        current.remove(x);
        //"guess" x is not in the subset
        generateSubset(superSet, k, idx + 1, current, solution);
    }


    private List<Set<T>> generateSubset(List<T> superSet, int k) {
        List<Set<T>> res = new ArrayList<>();
        generateSubset(superSet, k, 0, new HashSet<>(), res);
        return res;
    }

    /**
     * Генерирует подмножества не превышающие определенный размер
     *
     * @param set       - исходное множество
     * @param limitSize - ограничивающий размер
     * @return - Множество подмножеств, удовлетворяющее определенным условиям.
     */
    public Set<Set<T>> generateSubSets(Set<T> set, int limitSize) {
        Set<Set<T>> resultSet = new HashSet<>();
        List<T> list = new ArrayList<>();
        list.addAll(set);
        for (int i = 1; i <= limitSize; i++) {
            resultSet.addAll(generateSubset(list, i));
        }
        return resultSet;
    }
}
