package com.example.doormat_skeleton;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;
import java.util.Locale;

public class ChildResult {

    private ArrayList<ChildResult.DatabaseChildNode> data;

    //this allows us to return a HashSet of all doormats pulled from the database.
    public ArrayList<ChildResult.DatabaseChildNode> getData() {
        return data;
    }

    public void setData(ArrayList<ChildResult.DatabaseChildNode> data) {
        this.data = data;
    }

    public static class DatabaseChildNode {

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChildResult.DatabaseChildNode cn = (ChildResult.DatabaseChildNode) o;

            return getAnchor_id().equals(cn.getAnchor_id()) &&
                    getColor().equals(cn.getColor()) &&
                    getShape().equals(cn.getShape()) &&
                    getPosition().equals(cn.getPosition()) &&
                    getScale().equals(cn.getScale()) &&
                    getRotation().equals(cn.getRotation());

        }

//        @Override
//        public String toString() {
//            return "DatabaseChildNode{" +
//                    "anchor_id='" + anchor_id + '\'' +
//                    ", color='" + color + '\'' +
//                    ", shape='" + shape + '\'' +
//                    ", position=(" + String.format(Locale.US, "%.2f", position_vx) + ", "
//                    ", position_vy=" + position_vy +
//                    ", position_vz=" + position_vz +
//                    ", scale_vx=" + scale_vx +
//                    ", scale_vy=" + scale_vy +
//                    ", scale_vz=" + scale_vz +
//                    ", rotation_qx=" + rotation_qx +
//                    ", rotation_qy=" + rotation_qy +
//                    ", rotation_qz=" + rotation_qz +
//                    ", rotation_qw=" + rotation_qw +
//                    '}';
//        }
    }
}
