package dev.yaseen.booster.ramguard;

import android.os.FileObserver;
import android.util.Log;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
            Log.i(TAG, "SYSTEM_SERVER_CALLBACK_REACHED probe=0.2.1");
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
            String nonce;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(REQUEST_PATH),
                            StandardCharsets.UTF_8
                    ))) {
                nonce = reader.readLine();
            }
            if (nonce == null) {
                nonce = "";
            }
            nonce = nonce.trim();

            // Accept only a short alphanumeric request token.
            if (nonce.isEmpty() || nonce.length() > 64
                    || !nonce.matches("[A-Za-z0-9_-]+")) {
                Log.w(TAG, "PING_REJECTED invalid request token");
                return;
            }

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(RESPONSE_PATH, false),
                    StandardCharsets.UTF_8
            )) {
                writer.write("SUCCESSFUL:" + nonce + "\n");
                writer.flush();
            }
            Log.i(TAG, "PING_REPLY_SENT nonce=" + nonce);
        } catch (Throwable error) {
            Log.e(TAG, "PING_REPLY_FAILED", error);
        }
    }
}
