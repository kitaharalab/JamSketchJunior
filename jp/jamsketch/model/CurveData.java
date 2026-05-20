package jp.jamsketch.model;

import com.fasterxml.jackson.databind.annotation.NoClass;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class CurveData {
    final ArrayList<Point> curves = new ArrayList<>();


    public CurveData(){}

    public void add(Point p){
        Point target = get(p.x);
        if(target == null){
            curves.add(p);
        }else{
            curves.remove(target);
            curves.add(p);
        }
        curves.sort(Comparator.comparingInt(o -> o.x));
    }

    public ArrayList<Point> getAll(){
        return (ArrayList<Point>) curves.clone();
    }


    public Point get(int x){
        for(Point p : curves)
            if(p.x == x)
                return p;

        return null;
    }

    public void removeAll(){
        curves.clear();
    }

}
