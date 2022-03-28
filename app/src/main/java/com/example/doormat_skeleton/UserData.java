package com.example.doormat_skeleton;

import java.util.List;

//class for doormat objects.

public class UserData {

    private List<Doormat> data;

    //this allows us to return a list of all doormats pulled from the database.
    public List<Doormat> getData() {
        return data;
    }

    public void setData(List<Doormat> data) {
        this.data = data;
    }

    public static class Doormat {
        private int doormat_id;
        private double latitude;
        private double longitude;
        private String created_by;
        private String shape;
        private String color;

        public int getDoormat_id() {
            return doormat_id;
        }

        public void setDoormat_id(int doormat_id) {
            this.doormat_id = doormat_id;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public String getCreated_by() {
            return created_by;
        }

        public void setCreated_by(String created_by) {
            this.created_by = created_by;
        }

        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }
}