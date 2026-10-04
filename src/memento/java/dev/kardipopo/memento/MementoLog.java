package dev.kardipopo.memento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MementoLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(MementoClient.MOD_ID);

    private MementoLog() {}

    public static void info(String message) {
        LOGGER.info("[Memento] {}", message);
    }

    public static void warn(String message) {
        LOGGER.warn("[Memento] {}", message);
    }
}
