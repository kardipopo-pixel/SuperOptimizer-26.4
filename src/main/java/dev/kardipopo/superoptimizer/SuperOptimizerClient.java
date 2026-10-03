package dev.kardipopo.superoptimizer;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal, deliberately conservative bootstrap.
 * No mixins, renderer hooks, shader edits, or asynchronous world access are enabled
 * until they can be tested against the exact target client.
 */
public final class SuperOptimizerClient implements ClientModInitializer {
    public static final String MOD_ID = "superoptimizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("SuperOptimizer 26.4 alpha.2 baseline loaded. Experimental optimizations are disabled by design.");
    }
}
