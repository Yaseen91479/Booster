# Booster

Booster is an Android performance project.

## Development status

- RAM Guard: initial system-server callback probe is the first development milestone.
- R Cleaner: planned for a later phase, after RAM Guard is implemented and tested.

The initial probe will only verify that the module callback runs. It will not modify Android process priorities, memory settings, system properties, or files.
