package com.benedetto.game_engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    private String id;
    private String name;
    private int atkBonus;
    private int defBonus;
}