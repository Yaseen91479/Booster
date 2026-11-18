package dev.yaseen.booster.ramguard;

import android.os.FileObserver;
import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only lifecycle/IPC probe.
 *
 * The probe only observes a dedicated request file and writes a matching
 * response. It does not install framework hooks or change Android policies.
 */
public final class RamGuardEntry extends XposedModule {
    private static final String TAG = "Booster-RAMGuard";
    private static final String REQUEST_PATH =
            "/data/local/tmp/booster_ramguard_ping.request";
    private static final String RESPONSE_PATH =
            "/data/local/tmp/booster_ramguard_ping.response";
    private static final AtomicBoolean CALLBACK_RECORDED = new AtomicBoolean(false);
    private static final AtomicBoolean WATCHER_STARTED = new AtomicBoolean(false);

    private FileObserver requestObserver;

    public RamGuardEntry() {
        super();
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        if (CALLBACK_RECORDED.compareAndSet(false, true)) {
            Log.i(TAG, "SYSTEM_SERVER_CALLBACK_REACHED probe=0.2.0");
        }

        if (!WATCHER_STARTED.compareAndSet(false, true)) {
            return;
        }

        try {
            File request = new File(REQUEST_PATH);
            File directory = request.getParentFile();
            if (directory == null || !directory.isDirectory()) {
                throw new IllegalStateException("Request directory is unavailable");
            }

            requestObserver = new FileObserver(
                    directory.getAbsolutePath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO
            ) {
                @Override
                public void onEvent(int event, String path) {
                    if (path == null || !request.getName().equals(path)) {
                        return;
                    }
                    answerPing();
                }
            };
            requestObserver.startWatching();
            Log.i(TAG, "PING_LISTENER_READY path=" + REQUEST_PATH);
        } catch (Throwable error) {
            WATCHER_STARTED.set(false);
            Log.e(TAG, "PING_LISTENER_START_FAILED", error);
        }
    }

    private void answerPing() {
        try {
            String nonce = Files.readString(
                    new File(REQUEST_PATH).toPath(), StandardCharsets.UTF_8
            ).trim();

            // Accept only a short alphanumeric request token.
            if (nonce.isEmpty() || nonce.length() > 64
                    || !nonce.matches("[A-Za-z0-9_-]+")) {
                Log.w(TAG, "PING_REJECTED invalid request token");
                return;
            }

            Files.writeString(
                    new File(RESPONSE_PATH).toPath(),
                    "SUCCESSFUL:" + nonce + "\n",
                    StandardCharsets.UTF_8
            );
            Log.i(TAG, "PING_REPLY_SENT nonce=" + nonce);
        } catch (Throwable error) {
            Log.e(TAG, "PING_REPLY_FAILED", error);
        }
    }
}
