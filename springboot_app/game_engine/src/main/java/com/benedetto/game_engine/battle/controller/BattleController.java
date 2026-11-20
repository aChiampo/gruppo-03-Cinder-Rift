package com.benedetto.game_engine.battle.controller;

import com.benedetto.game_engine.battle.dto.PveBattleRequest;
import com.benedetto.game_engine.battle.dto.PveBattleResponse;
import com.benedetto.game_engine.battle.service.PveBattleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    private final PveBattleService pveBattleService;

    public BattleController(PveBattleService pveBattleService) {
        this.pveBattleService = pveBattleService;
    }

    @PostMapping("/pve/{id}")
    public ResponseEntity<PveBattleResponse> executePveBattle(
            @PathVariable("id") String id,
            @RequestBody(required = false) PveBattleRequest request) {
        if (request == null) {
            request = new PveBattleRequest();
        }
        request.setStageId(id);
        PveBattleResponse response = pveBattleService.executeBattle(request);
        return ResponseEntity.ok(response);
    }
}
