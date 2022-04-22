package com.example.doormat_skeleton;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

//class for doormat objects.
//class for doormat objects.

public class UserData {

    private HashSet<Doormat> data;
    private ArrayList<ChildNode> childData;

    //this allows us to return a HashSet of all doormats pulled from the database.
    public HashSet<Doormat> getData() {
        return data;
    }

    public ArrayList<ChildNode> getChildData() {
        return childData;
    }

    public void setData(HashSet<Doormat> data) {
        this.data = data;
    }

    public static class Doormat implements Comparable<Doormat> {

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
            return getAnchor_id().equals(doormat.getAnchor_id());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getAnchor_id());
        }
    }

    public static class ChildNode {

        //fields for the database table
        private String anchor_id;
        private String color;
        private String shape;
        private float position_vx;
        private float position_vy;
        private float position_vz;
        private float scale_vx;
        private float scale_vy;
        private float scale_vz;
        private float rotation_qx;
        private float rotation_qy;
        private float rotation_qz;
        private float rotation_qw;

        //getters and setters

        public String getAnchor_id() {
            return anchor_id;
        }

        public void setAnchor_id(String doormat_id) {
            this.anchor_id = anchor_id;
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

        public Vector3 getPosition() {
            return new Vector3(position_vx, position_vy, position_vz);
        }

        public Vector3 getScale() {
            return new Vector3(scale_vx, scale_vy, scale_vz);
        }

        public Quaternion getRotation() {
            return new Quaternion(rotation_qx, rotation_qy, rotation_qz, rotation_qw);
        }

    }
}