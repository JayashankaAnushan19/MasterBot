# systemd Services Fundamentals

Almost every embedded Linux robot brain (a companion computer running
sensor drivers, ROS nodes, or a control loop) needs those processes to start
reliably at boot and restart if they crash. `systemd` is the init system and
service manager that does this on nearly every modern Linux distro,
including the ones running on typical robotics single-board computers.

## Unit files

A **unit file** (`.service`, ending in `/etc/systemd/system/*.service`)
describes one manageable process: what to run, when to start it, what it
depends on, and what to do if it fails.

```ini
[Unit]
Description=Lidar driver
After=network.target

[Service]
ExecStart=/usr/local/bin/lidar_driver
Restart=on-failure
RestartSec=2

[Install]
WantedBy=multi-user.target
```

## Service types

The `Type=` directive tells systemd how to know the service actually
started:

- **simple** (default): the process started by `ExecStart` *is* the
  service; systemd considers it started as soon as it forks.
- **oneshot**: the process is expected to exit before systemd considers the
  unit "active" — for a setup/init task, not a long-running daemon.
- **forking**: the process forks and the parent exits, leaving a child
  running in the background (older daemon style); systemd needs a `PIDFile`
  to track the real process.

Picking the wrong type is a common source of "my service shows as started
but the actual process isn't running yet" bugs.

## Restart policies

`Restart=on-failure` (or `always`) plus `RestartSec=` is what makes a sensor
driver or control node resilient to crashes without a human watching it —
critical for a robot running unattended. Without a restart policy, a crashed
node just... stays crashed.

## Enabling vs. starting

- `systemctl start <service>`: runs it now, this boot only.
- `systemctl enable <service>`: makes it start automatically on future
  boots (creates a symlink into the target's `.wants/` directory).
- You usually want **both** for a robot's core services: `enable` so it
  survives a power cycle, and `start` (or a reboot) to actually run it now.

## Debugging: journalctl

`journalctl -u <service>` shows that service's logs — the first place to
look when a service is failing to start or crash-looping. `journalctl -u
<service> -f` follows it live, useful while debugging a driver on actual
hardware.

## Quick reference

| Command | Effect |
|---|---|
| `systemctl start X` | run now |
| `systemctl enable X` | run on future boots |
| `systemctl status X` | current state + recent log lines |
| `journalctl -u X -f` | live log tail for X |
