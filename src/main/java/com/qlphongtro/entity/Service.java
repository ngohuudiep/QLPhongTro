package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "service_code", nullable = false, length = 20)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "calc_type", nullable = false, length = 20)
    private String calcType;

    // Quan hệ 1-Nhiều với Room_Services (Dịch vụ này được gán cho những phòng nào)
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL)
    private List<RoomService> roomServices = new ArrayList<>();

    public Service() {
    }

    public Service(Integer serviceId, String serviceCode, String serviceName, String unit, String calcType, List<RoomService> roomServices) {
        this.serviceId = serviceId;
        this.serviceCode = serviceCode;
        this.serviceName = serviceName;
        this.unit = unit;
        this.calcType = calcType;
        this.roomServices = roomServices != null ? roomServices : new ArrayList<>();
    }

    public Integer getServiceId() {
        return serviceId;
    }

    public void setServiceId(Integer serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCalcType() {
        return calcType;
    }

    public void setCalcType(String calcType) {
        this.calcType = calcType;
    }

    public List<RoomService> getRoomServices() {
        return roomServices;
    }

    public void setRoomServices(List<RoomService> roomServices) {
        this.roomServices = roomServices;
    }

    public static class ServiceBuilder {
        private Integer serviceId;
        private String serviceCode;
        private String serviceName;
        private String unit;
        private String calcType;
        private List<RoomService> roomServices = new ArrayList<>();

        ServiceBuilder() {}

        public ServiceBuilder serviceId(Integer serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public ServiceBuilder serviceCode(String serviceCode) {
            this.serviceCode = serviceCode;
            return this;
        }

        public ServiceBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public ServiceBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public ServiceBuilder calcType(String calcType) {
            this.calcType = calcType;
            return this;
        }

        public ServiceBuilder roomServices(List<RoomService> roomServices) {
            this.roomServices = roomServices;
            return this;
        }

        public Service build() {
            return new Service(serviceId, serviceCode, serviceName, unit, calcType, roomServices);
        }
    }

    public static ServiceBuilder builder() {
        return new ServiceBuilder();
    }
}
