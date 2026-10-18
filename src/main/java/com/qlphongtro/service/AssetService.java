package com.qlphongtro.service;

import com.qlphongtro.entity.Asset;
import java.util.List;

public interface AssetService {

    List<Asset> getAllAssets();

    Asset getAssetById(Integer id);

    List<Asset> getAssetsByRoomId(Integer roomId);

    Asset saveAsset(Asset asset);

    void deleteAsset(Integer id);
}
