package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Service;
import com.qlphongtro.repository.ServiceRepository;
import com.qlphongtro.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@Transactional
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;

    @Autowired
    public ServiceServiceImpl(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Service getServiceById(Integer id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ ID: " + id));
    }

    @Override
    public Service saveService(Service service) {
        return serviceRepository.save(service);
    }

    @Override
    public void deleteService(Integer id) {
        if (!serviceRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa, dịch vụ không tồn tại ID: " + id);
        }
        serviceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByServiceCode(String serviceCode) {
        return serviceRepository.existsByServiceCode(serviceCode);
    }
}
