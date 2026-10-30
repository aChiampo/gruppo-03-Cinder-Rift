package com.benedetto.game_engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class RoundResult {
    private List<String> log;
    private Player winner;
}