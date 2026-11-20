package com.benedetto.game_engine.model;

import lombok.AllArgsConstructor;
import com.benedetto.game_engine.effect.CardEffect;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    private Integer id;
    private String name;
    private List<CardEffect> effects = List.of();
}
