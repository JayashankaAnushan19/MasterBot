# IMU Fundamentals

An IMU (Inertial Measurement Unit) is how a robot knows its own orientation
and motion without looking at the outside world. Almost every mobile robot,
drone, and arm end-effector has one â and almost every one of them drifts,
which is the central problem you're actually managing.

## What's inside

A typical IMU bundles:

- **Accelerometer** (3-axis): measures linear acceleration, including gravity.
  At rest, it reads ~1g pointing "down" relative to the sensor â this is
  actually how you can extract absolute pitch/roll from it (gravity is a
  known reference direction).
- **Gyroscope** (3-axis): measures angular velocity (rate of rotation) about
  each axis, not absolute angle.
- **Magnetometer** (3-axis, often included): measures magnetic field
  direction, used as a compass reference for absolute heading (yaw).
  Together these three make a "9-DOF" IMU.

## Why each sensor alone isn't enough

- **Gyroscope**: to get orientation you must **integrate** angular velocity
  over time. Any small constant sensor bias/noise integrates into
  ever-growing error â this is **gyro drift**, and it's unavoidable over
  long time horizons with the gyro alone.
- **Accelerometer**: gives an absolute pitch/roll reference from gravity, but
  it's noisy and, worse, can't tell the difference between gravity and actual
  linear acceleration from the robot moving. During dynamic motion (braking,
  turning), its "which way is down" estimate becomes unreliable.
- **Magnetometer**: gives absolute yaw, but is easily distorted by nearby
  ferrous metal or current-carrying wires â common right next to motors and
  power electronics, exactly where robots put them.

## Sensor fusion

Because each sensor is good at what the others are bad at, IMU data is
**fused**, not used raw:

- **Complementary filter**: a simple, cheap fusion â trust the gyro's
  short-term relative change (low-pass the accelerometer, high-pass the
  gyro), correcting slow gyro drift with the accelerometer's long-term
  absolute reference. Easy to implement, runs on the smallest microcontroller.
- **Kalman filter (or Extended/EKF)**: a statistically principled fusion that
  weights each sensor by its estimated uncertainty and propagates a full
  state estimate (orientation, and often velocity/position) over time. More
  compute, better accuracy, and it's what most real robot state estimation
  (e.g. `robot_localization` in ROS2) uses under the hood.

## Practical gotchas

- **Calibration matters**: raw accelerometer/gyro readings have per-unit bias
  and scale error; skipping calibration bakes constant error into every
  downstream estimate.
- **Mounting orientation and vibration**: a rigidly-mounted IMU picks up
  motor/frame vibration as noise; soft-mounting (with damping) trades a bit
  of responsiveness for a much cleaner signal.
- IMU alone can never give you **absolute position** â only orientation and
  relative motion. Position requires integrating twice (which drifts badly)
  or fusing in an external reference (GPS, visual odometry, wheel encoders).

## Quick reference

| Sensor | Measures | Strength | Weakness |
|---|---|---|---|
| Accelerometer | linear acceleration | absolute pitch/roll (via gravity) | noisy, confused by dynamic motion |
| Gyroscope | angular velocity | accurate short-term relative rotation | drifts when integrated over time |
| Magnetometer | magnetic field direction | absolute yaw reference | distorted by nearby metal/current |
