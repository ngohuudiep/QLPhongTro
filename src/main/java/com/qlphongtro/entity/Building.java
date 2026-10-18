package com.qlphongtro.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Buildings")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Integer buildingId;

    @Column(name = "building_code", nullable = false, length = 20)
    private String buildingCode;

    @Column(name = "building_name", nullable = false, length = 150)
    private String buildingName;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "total_floors")
    private Short totalFloors;

    public Building() {
    }

    public Building(Integer buildingId, String buildingCode, String buildingName, String address, String city, Short totalFloors) {
        this.buildingId = buildingId;
        this.buildingCode = buildingCode;
        this.buildingName = buildingName;
        this.address = address;
        this.city = city;
        this.totalFloors = totalFloors;
    }

    public Integer getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Integer buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingCode() {
        return buildingCode;
    }

    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Short getTotalFloors() {
        return totalFloors;
    }

    public void setTotalFloors(Short totalFloors) {
        this.totalFloors = totalFloors;
    }

    public static class BuildingBuilder {
        private Integer buildingId;
        private String buildingCode;
        private String buildingName;
        private String address;
        private String city;
        private Short totalFloors;

        BuildingBuilder() {}

        public BuildingBuilder buildingId(Integer buildingId) {
            this.buildingId = buildingId;
            return this;
        }

        public BuildingBuilder buildingCode(String buildingCode) {
            this.buildingCode = buildingCode;
            return this;
        }

        public BuildingBuilder buildingName(String buildingName) {
            this.buildingName = buildingName;
            return this;
        }

        public BuildingBuilder address(String address) {
            this.address = address;
            return this;
        }

        public BuildingBuilder city(String city) {
            this.city = city;
            return this;
        }

        public BuildingBuilder totalFloors(Short totalFloors) {
            this.totalFloors = totalFloors;
            return this;
        }

        public Building build() {
            return new Building(buildingId, buildingCode, buildingName, address, city, totalFloors);
        }
    }

    public static BuildingBuilder builder() {
        return new BuildingBuilder();
    }
}
