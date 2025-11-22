package Rover;

import Utils.Point3D;

import java.util.ArrayList;
import java.util.List;

public class Base { // charge rovers battery
    private List <String> items = new ArrayList<>();
    private Point3D position;

    public Base(Point3D position) {
        this.position = position;
    }
    public Point3D getPosition() {
        return this.position;
    }
    public void addItem(String item) {items.add(item);}

}
