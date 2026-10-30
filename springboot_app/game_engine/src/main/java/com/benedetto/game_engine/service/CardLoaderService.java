package com.benedetto.game_engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.benedetto.game_engine.model.PlayerDeck;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CardLoaderService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;

    public CardLoaderService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public PlayerDeck loadDeck(String resourcePath) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
            return objectMapper.readValue(resource.getInputStream(), PlayerDeck.class);
        } catch (IOException e) {
            throw new RuntimeException("Errore nel caricamento del file mazzo: " + resourcePath, e);
        }
    }
}