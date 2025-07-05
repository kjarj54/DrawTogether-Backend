package com.drawtogether.model;

public class DrawData {
    private final double x;
    private final double y;
    private final String color;
    private final double strokeWidth;
    private final String tool;

    public DrawData(String color, double strokeWidth, double x, double y, String tool) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.x = x;
        this.y = y;
        this.tool = tool;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getColor() {
        return color;
    }

    public double getStrokeWidth() {
        return strokeWidth;
    }

    public String getTool() {
        return tool;
    }

}
