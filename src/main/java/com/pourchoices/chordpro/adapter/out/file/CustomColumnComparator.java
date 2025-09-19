package com.pourchoices.chordpro.adapter.out.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class CustomColumnComparator implements Comparator<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomColumnComparator.class);

    private final List<String> desiredOrder;

    public CustomColumnComparator(List<String> desiredOrder) {
        this.desiredOrder = desiredOrder;
    }

    @Override
    public int compare(String col1, String col2) {

        // FIXME the header names are getting uppercased so need to track that down
        int index1 = desiredOrder.indexOf(col1.toLowerCase());
        int index2 = desiredOrder.indexOf(col2.toLowerCase());

        // Handle cases where a column is not in the desiredOrder list
        if (index1 == -1) {
            index1 = Integer.MAX_VALUE;
        }
        if (index2 == -1) {
            index2 = Integer.MAX_VALUE;
        }

        return Integer.compare(index1, index2);
    }
}
