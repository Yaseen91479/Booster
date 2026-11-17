package dev.yaseen.booster.ramguard;

import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only milestone probe.
 *
 * This class deliberately installs no hooks and does not change process,
 * memory, scheduler, system-property, filesystem, or Android framework state.
 */
public final class RamGuardEntry extends XposedModule {
    private static final String TAG = "Booster-RAMGuard";
    private static final AtomicBoolean CALLBACK_RECORDED = new AtomicBoolean(false);

    public RamGuardEntry() {
        super();
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        if (!CALLBACK_RECORDED.compareAndSet(false, true)) {
            return;
        }

        try {
            Log.i(TAG,
                "SYSTEM_SERVER_CALLBACK_REACHED probe=0.1.0 " +
                "hooks=none policy_changes=none mode=read_only");
        } catch (Throwable ignored) {
            // Fail-open: logging failure must never affect system_server startup.
        }
    }
}
