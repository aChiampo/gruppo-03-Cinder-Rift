package com.benedetto.game_engine.battle.controller;

import com.benedetto.game_engine.battle.dto.PveBattleRequest;
import com.benedetto.game_engine.battle.dto.PveBattleResponse;
import com.benedetto.game_engine.battle.dto.PvpBattleRequest;
import com.benedetto.game_engine.battle.dto.PvpBattleResponse;
import com.benedetto.game_engine.battle.service.PveBattleService;
import com.benedetto.game_engine.battle.service.PvpBattleService;
import com.benedetto.game_engine.battle.BattleDeadline;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    private final PveBattleService pveBattleService;
    private final PvpBattleService pvpBattleService;

    public BattleController(PveBattleService pveBattleService, PvpBattleService pvpBattleService) {
        this.pveBattleService = pveBattleService;
        this.pvpBattleService = pvpBattleService;
    }

    @PostMapping("/pve/{id}")
    public ResponseEntity<PveBattleResponse> executePveBattle(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Battle-Deadline", required = false) String deadline,
            @RequestBody(required = false) PveBattleRequest request) {
        BattleDeadline checkpoint = BattleDeadline.fromHeader(deadline);
        if (request == null) {
            request = new PveBattleRequest();
        }
        request.setStageId(id);
        PveBattleResponse response = pveBattleService.executeBattle(request, checkpoint);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pvp")
    public ResponseEntity<PvpBattleResponse> executePvpBattle(
            @RequestHeader(value = "X-Battle-Deadline", required = false) String deadline,
            @Valid @RequestBody PvpBattleRequest request) {
        BattleDeadline checkpoint = BattleDeadline.fromHeader(deadline);
        PvpBattleResponse response = pvpBattleService.executeBattle(request, checkpoint);
        return ResponseEntity.ok(response);
    }
}
