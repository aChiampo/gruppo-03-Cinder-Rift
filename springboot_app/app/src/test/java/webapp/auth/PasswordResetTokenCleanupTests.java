package webapp.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.support.SimpleTriggerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PasswordResetTokenCleanupTests {
    @Test
    void recoveryCleanupDefaultsToThreeUtc() {
        try (var context = context(Map.of())) {
            var task = (CronTask) context.getBean(ScheduledAnnotationBeanPostProcessor.class)
                    .getScheduledTasks().iterator().next().getTask();
            assertThat(task.getTrigger().nextExecution(new SimpleTriggerContext(
                    Clock.fixed(Instant.parse("2026-11-12T02:59:59Z"), ZoneOffset.UTC))))
                    .isEqualTo(Instant.parse("2026-11-12T03:00:00Z"));
        }
    }

    @Test
    void recoveryCleanupHasItsOwnScheduleAndTimeZone() {
        try (var context = context(Map.of("app.security.password-reset-token-cleanup-cron", "0 15 9 * * *",
                "app.security.password-reset-token-cleanup-zone", "Asia/Tokyo"))) {
            var task = (CronTask) context.getBean(ScheduledAnnotationBeanPostProcessor.class)
                    .getScheduledTasks().iterator().next().getTask();
            assertThat(task.getTrigger().nextExecution(new SimpleTriggerContext(
                    Clock.fixed(Instant.parse("2026-11-12T00:14:59Z"), ZoneOffset.UTC))))
                    .isEqualTo(Instant.parse("2026-11-12T00:15:00Z"));
        }
    }

    @Test
    void recoveryCleanupCanBeDisabledIndependently() {
        try (var context = context(Map.of("app.security.password-reset-token-cleanup-cron", "-"))) {
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).isEmpty();
        }
    }

    private static AnnotationConfigApplicationContext context(Map<String, Object> properties) {
        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(new MockEnvironment());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("reset-cleanup-test", properties));
        context.register(SchedulingConfig.class);
        context.registerBean(AccountRepository.class, () -> mock(AccountRepository.class));
        context.registerBean(Clock.class, Clock::systemUTC);
        context.registerBean(PasswordResetTokenCleanup.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingConfig {}
}
