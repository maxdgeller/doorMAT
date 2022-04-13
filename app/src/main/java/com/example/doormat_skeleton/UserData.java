package com.example.doormat_skeleton;

import java.util.HashSet;
import java.util.Objects;

//class for doormat objects.
//class for doormat objects.

public class UserData {

    private HashSet<Doormat> data;

    //this allows us to return a HashSet of all doormats pulled from the database.
    public HashSet<Doormat> getData() {
        return data;
    }

    public void setData(HashSet<Doormat> data) {
        this.data = data;
    }

    public static class Doormat implements Comparable<Doormat> {

        //fields for the database table
        private String doormat_id;
        private double latitude;
        private double longitude;
        private String created_by;
        private String shape;
        private String color;

        //fields not to be in the database
        //proximity to user; should be -1 except when manually setting it for comparison purposes
        private double proximity = -1;
        //whether it's been found or not
        private boolean found = false;


        //getters and setters

        public String getDoormat_id() {
            return doormat_id;
        }

        public void setDoormat_id(String doormat_id) {
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

        public double getProximity() {
            return proximity;
        }

        public void setProximity(double distance) {
            proximity = distance;
        }

        public boolean isFound() {
            return found;
        }

        public void setFound(boolean newFound) {
            found = newFound;
        }

        //proximity of -1 is not a real proximity, so it goes at the end
        @Override
        public int compareTo(Doormat d) {
            if (getProximity() == d.getProximity()) {
                return 0;
            }
            if (getProximity() > d.getProximity() || getProximity() == -1) {
                return 1;
            }
            return -1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Doormat doormat = (Doormat) o;
            return getDoormat_id().equals(doormat.getDoormat_id());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDoormat_id());
        }
    }
}