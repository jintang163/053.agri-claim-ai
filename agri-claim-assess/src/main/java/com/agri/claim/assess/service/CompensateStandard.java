package com.agri.claim.assess.service;

import com.agri.claim.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CompensateStandard {

    private final Map<String, CropStandard> cropStandardMap = new HashMap<>() {{
        put("小麦", new CropStandard("小麦", 800, 2.8, 0.8, 0.3, 0.6, 1.0));
        put("玉米", new CropStandard("玉米", 900, 2.5, 0.75, 0.35, 0.65, 1.0));
        put("水稻", new CropStandard("水稻", 1000, 3.2, 0.85, 0.3, 0.6, 1.0));
        put("大豆", new CropStandard("大豆", 350, 5.5, 0.7, 0.3, 0.6, 1.0));
        put("棉花", new CropStandard("棉花", 250, 18.0, 0.7, 0.35, 0.65, 1.0));
        put("蔬菜", new CropStandard("蔬菜", 3000, 4.0, 0.75, 0.25, 0.55, 1.0));
        put("水果", new CropStandard("水果", 2000, 6.0, 0.7, 0.3, 0.6, 1.0));
        put("油菜", new CropStandard("油菜", 200, 6.5, 0.75, 0.3, 0.6, 1.0));
        put("花生", new CropStandard("花生", 300, 9.0, 0.7, 0.35, 0.65, 1.0));
        put("烟草", new CropStandard("烟草", 180, 35.0, 0.7, 0.3, 0.6, 1.0));
    }};

    private final Map<String, BigDecimal> disasterCoeffMap = new HashMap<>() {{
        put(Constants.DISASTER_LEVEL_LIGHT, BigDecimal.valueOf(0.30));
        put(Constants.DISASTER_LEVEL_MODERATE, BigDecimal.valueOf(0.60));
        put(Constants.DISASTER_LEVEL_SEVERE, BigDecimal.valueOf(0.95));
    }};

    public CropStandard getCropStandard(String cropType) {
        return cropStandardMap.getOrDefault(cropType,
                new CropStandard(cropType, 500, 5.0, 0.7, 0.3, 0.6, 1.0));
    }

    public BigDecimal getDisasterCoeff(String disasterLevel) {
        return disasterCoeffMap.getOrDefault(disasterLevel, BigDecimal.valueOf(0.5));
    }

    public record CropStandard(String cropType, double unitYieldKg, double unitPriceYuan,
                                double compensateRatio, double lightThreshold,
                                double moderateThreshold, double severeThreshold) {}
}
