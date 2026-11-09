package com.benedetto.game_campaign.service;

import com.benedetto.game_campaign.model.CampaignStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CampaignStageLoaderService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;

    public CampaignStageLoaderService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public CampaignStage loadStage(String stagePath) {
        try {
            String fullPath;
            if (stagePath.startsWith("classpath:")) {
                fullPath = stagePath;
            } else if (stagePath.startsWith("campaign/stages/")) {
                fullPath = "classpath:" + stagePath;
            } else if (stagePath.startsWith("/campaign/stages/")) {
                fullPath = "classpath:" + stagePath.substring(1);
            } else {
                fullPath = "classpath:campaign/stages/" + stagePath;
            }
            Resource resource = resourceLoader.getResource(fullPath);
            return objectMapper.readValue(resource.getInputStream(), CampaignStage.class);
        } catch (IOException e) {
            throw new RuntimeException("Errore nel caricamento del file stage: " + stagePath, e);
        }
    }
}
