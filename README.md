# Booster

Booster is an Android performance project.

## RAM Guard Probe 0.1.0

The first milestone is a read-only verification that the libxposed system-server lifecycle callback is reached.

### Safety properties

- No ART hooks are installed.
- No process priorities, OOM scores, memory settings, scheduler settings, or system properties are changed.
- No files or persistent system configuration are modified.
- The callback emits one informational log line per module generation.
- Logging failures are swallowed so they cannot interrupt system-server startup.
- The module scope is restricted to the special `system` target.
- The module uses libxposed's protective exception mode.
- R Cleaner is not included and remains deferred until RAM Guard is implemented and tested.

Expected log:

```
SYSTEM_SERVER_CALLBACK_REACHED probe=0.1.0 hooks=none policy_changes=none mode=read_only
```

### Build

GitHub Actions builds debug and release APKs on pushes affecting the Android module and on manual workflow dispatch. Download the `ram-guard-probe-apks` artifact from the workflow run.

### Device activation notes

1. Keep the existing root and Zygisk/Vector setup working.
2. Install the generated APK in Vector's module manager and enable its `system` scope.
3. Since the temporary DirtyFrag root session is lost on a full reboot, do not use a full reboot just to activate this test. Use the soft-reboot action you normally use after enabling Vector modules.
4. A soft reboot restarts Android's runtime/system_server; it does not guarantee DirtyFrag root survives every device configuration. If the temporary root session is lost, stop and restore the supported root session before testing.
5. Verify the expected log before treating the callback milestone as passed.

This probe is not the RAM Guard policy engine and does not yet protect apps from memory pressure.
