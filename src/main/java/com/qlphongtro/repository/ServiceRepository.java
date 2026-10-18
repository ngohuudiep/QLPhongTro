package com.qlphongtro.repository;

import com.qlphongtro.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {

    Optional<Service> findByServiceCode(String serviceCode);

    boolean existsByServiceCode(String serviceCode);
}
