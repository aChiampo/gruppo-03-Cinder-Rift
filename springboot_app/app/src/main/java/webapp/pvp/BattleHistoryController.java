package webapp.pvp;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import webapp.auth.UserProfile;

/** Exposes read access to the authenticated player's battle history. */
@RestController
@RequestMapping("/api/battles")
public class BattleHistoryController {
    private final BattleHistoryService service;

    public BattleHistoryController(BattleHistoryService service) {
        this.service = service;
    }

    @GetMapping("/history")
    public List<BattleHistoryEntry> history(
            @AuthenticationPrincipal UserProfile user,
            @RequestParam(required = false) String mode) {
        return service.listHistory(user.id(), mode);
    }
}
