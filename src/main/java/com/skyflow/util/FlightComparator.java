package com.skyflow.util;

import com.skyflow.model.Flight;
import java.util.Comparator;

public class FlightComparator implements Comparator<Flight> {

    @Override
    public int compare(Flight flight1, Flight flight2) {
        // Higher priority values should come first in the queue, So we reverse the natural order comparison
        return Integer.compare(flight2.getPriority(), flight1.getPriority());
    }
}