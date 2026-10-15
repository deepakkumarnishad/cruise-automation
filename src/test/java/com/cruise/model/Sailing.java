package com.cruise.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class Sailing {
    public String title;          
    public int nights = -1;       
    public LocalDate start;
    public LocalDate end;
    public int price = -1;       
    public List<String> ports = new ArrayList<>();
    public String rawText;

    @Override
    public String toString() {
        return "Sailing{title='" + title + "', nights=" + nights + ", start=" + start + ", end=" + end
                + ", price=" + price + ", ports=" + ports + "}";
    }
}
