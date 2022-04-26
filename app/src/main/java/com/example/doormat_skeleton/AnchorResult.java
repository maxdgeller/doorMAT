package com.example.doormat_skeleton;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class AnchorResult {

    private HashSet<DatabaseAnchor> data;

    //this allows us to return a HashSet of all doormats pulled from the database.
    public HashSet<DatabaseAnchor> getData() {
        return data;
    }

    public void setData(HashSet<DatabaseAnchor> data) {
        this.data = data;
    }

    public static class DatabaseAnchor implements Comparable<DatabaseAnchor> {

        //fields for the database table
        private String anchor_id;
        private String color;
        private String shape;
        private double latitude;
        private double longitude;
        private String created_by;


        //fields not to be in the database
        //proximity to user; should be -1 except when manually setting it for comparison purposes
        private double proximity = -1;
        //whether it's been found or not
        private boolean found = false;


        //getters and setters

        public String getAnchor_id() {
            return anchor_id;
        }

        public void setAnchor_id(String anchor_id) {
            this.anchor_id = anchor_id;
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
        public int compareTo(DatabaseAnchor d) {
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
            DatabaseAnchor databaseAnchor = (DatabaseAnchor) o;
            return getAnchor_id().equals(databaseAnchor.getAnchor_id());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getAnchor_id());
        }
    }
}