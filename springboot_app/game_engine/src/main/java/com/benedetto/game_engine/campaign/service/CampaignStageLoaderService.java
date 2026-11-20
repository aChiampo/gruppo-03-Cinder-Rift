package com.benedetto.game_engine.campaign.service;

import com.benedetto.game_engine.battle.exception.StageNotFoundException;
import com.benedetto.game_engine.campaign.model.CampaignStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public CampaignStage loadStageByIdentifier(String id) {
        if (id == null || id.isBlank()) {
            throw new StageNotFoundException(id, "Stage identifier cannot be null or empty");
        }

        String trimmedId = id.trim();
        Integer numericId = null;
        try {
            numericId = Integer.parseInt(trimmedId);
        } catch (NumberFormatException ignored) {
        }

        List<Resource> stageResources = discoverStageResources();
        for (Resource resource : stageResources) {
            try {
                CampaignStage stage = objectMapper.readValue(resource.getInputStream(), CampaignStage.class);
                if (stage == null) {
                    continue;
                }

                if (numericId != null) {
                    if (stage.getStageNumber() == numericId) {
                        return stage;
                    }
                } else {
                    if (stage.getStageId() != null && stage.getStageId().equalsIgnoreCase(trimmedId)) {
                        return stage;
                    }
                    String filename = resource.getFilename();
                    if (filename != null) {
                        if (filename.equalsIgnoreCase(trimmedId) || filename.equalsIgnoreCase(trimmedId + ".json")) {
                            return stage;
                        }
                        String baseName = filename.endsWith(".json")
                                ? filename.substring(0, filename.length() - 5)
                                : filename;
                        if (baseName.equalsIgnoreCase(trimmedId)) {
                            return stage;
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        throw new StageNotFoundException(trimmedId, "No campaign stage found matching identifier: " + trimmedId);
    }

    private List<Resource> discoverStageResources() {
        List<Resource> result = new ArrayList<>();
        try {
            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources("classpath*:campaign/stages/*.json");
            if (resources == null || resources.length == 0) {
                resources = resolver.getResources("classpath:campaign/stages/*.json");
            }
            if (resources != null && resources.length > 0) {
                result.addAll(Arrays.asList(resources));
            }
        } catch (IOException ignored) {
        }

        if (result.isEmpty()) {
            List<String> defaultStagePaths = List.of(
                    "campaign/stages/stage_01_fazioni.json",
                    "campaign/stages/stage_03_evento_speciale.json"
            );
            for (String path : defaultStagePaths) {
                Resource res = resourceLoader.getResource("classpath:" + path);
                if (res.exists()) {
                    result.add(res);
                }
            }
        }
        return result;
    }
}
