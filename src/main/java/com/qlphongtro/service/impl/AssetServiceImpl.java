package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Asset;
import com.qlphongtro.repository.AssetRepository;
import com.qlphongtro.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    @Autowired
    public AssetServiceImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Asset getAssetById(Integer id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài sản ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> getAssetsByRoomId(Integer roomId) {
        return assetRepository.findByRoom_RoomId(roomId);
    }

    @Override
    public Asset saveAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    @Override
    public void deleteAsset(Integer id) {
        if (!assetRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy tài sản ID: " + id);
        }
        assetRepository.deleteById(id);
    }
}
