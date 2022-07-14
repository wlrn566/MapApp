package com.wlrn566.gpsTracker.VO;

import androidx.annotation.NonNull;

public class RestaurantVO {
    String seq;
    String name;
    Double latitude;
    Double longitude;
    String crt_dt;

    public RestaurantVO(String seq, String name, Double latitude, Double longitude, String crt_dt) {
        this.seq = seq;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.crt_dt = crt_dt;
    }

    @Override
    public String toString() {
        return "RestaurantVO{" +
                "seq='" + seq + '\'' +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", crt_dt='" + crt_dt + '\'' +
                '}';
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCrt_dt() {
        return crt_dt;
    }

    public void setCrt_dt(String crt_dt) {
        this.crt_dt = crt_dt;
    }
}
