package webapp.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.support.SimpleTriggerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SessionTokenCleanupTests {
    @Test
    void defaultScheduleRunsDailyAtThreeUtc() {
        try (var context = context(Map.of())) {
            CronTask task = scheduledTask(context);
            assertThat(task.getExpression()).isEqualTo("0 0 3 * * *");
            assertThat(task.getTrigger().nextExecution(new SimpleTriggerContext(
                    Clock.fixed(Instant.parse("2026-11-12T02:59:59Z"), ZoneOffset.UTC))))
                    .isEqualTo(Instant.parse("2026-11-12T03:00:00Z"));
        }
    }

    @Test
    void scheduleAndTimeZoneCanBeOverridden() {
        try (var context = context(Map.of(
                "app.security.session-token-cleanup-cron", "0 15 9 * * *",
                "app.security.session-token-cleanup-zone", "Asia/Tokyo"))) {
            assertThat(scheduledTask(context).getTrigger().nextExecution(new SimpleTriggerContext(
                    Clock.fixed(Instant.parse("2026-11-12T00:14:59Z"), ZoneOffset.UTC))))
                    .isEqualTo(Instant.parse("2026-11-12T00:15:00Z"));
        }
    }

    @Test
    void dashDisablesTheScheduledJob() {
        try (var context = context(Map.of("app.security.session-token-cleanup-cron", "-"))) {
            assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks()).isEmpty();
        }
    }

    private static AnnotationConfigApplicationContext context(Map<String, Object> properties) {
        var context = new AnnotationConfigApplicationContext();
        // Keep the default-schedule test independent of deployment overrides such as cron='-'.
        context.setEnvironment(new MockEnvironment());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("cleanup-test", properties));
        context.register(SchedulingTestConfig.class);
        context.refresh();
        return context;
    }

    private static CronTask scheduledTask(AnnotationConfigApplicationContext context) {
        var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
        assertThat(tasks).hasSize(1);
        return (CronTask) tasks.iterator().next().getTask();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableScheduling
    @Import(SessionTokenCleanup.class)
    static class SchedulingTestConfig {
        @Bean
        AccountRepository accounts() {
            return mock(AccountRepository.class);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
