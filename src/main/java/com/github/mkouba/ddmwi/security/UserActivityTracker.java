package com.github.mkouba.ddmwi.security;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduler;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class UserActivityTracker {

    private static final Logger LOG = Logger.getLogger(UserActivityTracker.class);

    private final ConcurrentMap<String, Instant> lastActivity = new ConcurrentHashMap<>();

    @ConfigProperty(name = "quarkus.http.auth.form.timeout")
    Duration cookieTimeout;

    @Inject
    Event<UserRemoved> userRemoved;

    public List<Map.Entry<String, LocalDateTime>> getLastActivity() {
        return lastActivity.entrySet().stream()
                .map(e -> Map.entry(e.getKey(),
                        LocalDateTime.ofInstant(e.getValue(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)))
                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .collect(Collectors.toList());
    }

    public void remove(String username) {
        if (lastActivity.remove(username) != null) {
            userRemoved.fire(new UserRemoved(username));
        }
    }

    void update(String username) {
        LOG.debugf("User activity update: %s", username);
        lastActivity.put(username, Instant.now());
    }

    void checkInactiveUsers(@Observes StartupEvent event, Scheduler scheduler) {
        scheduler.newJob("inactiveUsers")
                .setInterval(cookieTimeout.plus(cookieTimeout).toString())
                .setTask(se -> {
                    LOG.infof("Checking inactive users, next fire time: %s",
                            LocalDateTime.ofInstant(se.getTrigger().getNextFireTime(), ZoneId.systemDefault()));
                    Instant now = Instant.now();
                    // The entry is removed if the duration between the last activity and now is > timeout
                    lastActivity.entrySet().removeIf(e -> {
                        boolean ret = !Duration.between(e.getValue(), now).minus(cookieTimeout).isNegative();
                        if (ret) {
                            LOG.debugf("Removing inactive user: %s", e.getKey());
                            userRemoved.fire(new UserRemoved(e.getKey()));
                        }
                        return ret;
                    });
                }).schedule();
    }

    public record UserRemoved(String username) {
    }

}
