package com.qlphongtro.service;

import com.qlphongtro.entity.Service;
import java.util.List;

public interface ServiceService {

    List<Service> getAllServices();

    Service getServiceById(Integer id);

    Service saveService(Service service);

    void deleteService(Integer id);

    boolean existsByServiceCode(String serviceCode);
}
