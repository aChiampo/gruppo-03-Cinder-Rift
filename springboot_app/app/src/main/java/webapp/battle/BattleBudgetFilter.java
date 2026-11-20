package webapp.battle;

import java.io.IOException;
import java.time.Clock;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Starts the timing budget before authentication and request deserialization. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class BattleBudgetFilter extends OncePerRequestFilter {
    private final Clock clock;

    public BattleBudgetFilter(Clock clock) { this.clock = clock; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !request.getServletPath().startsWith("/api/battle/pve/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        request.setAttribute(BattleBudget.ATTRIBUTE, BattleBudget.start(clock));
        chain.doFilter(request, response);
    }
}
