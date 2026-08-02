# DC Motor Fundamentals

Almost every mobile robot and arm joint comes down to a DC motor at the end of
the chain. Picking and driving one correctly means understanding its
torque-speed tradeoff, not just its voltage rating.

## How it works

A DC motor produces torque proportional to current through its windings, via
the interaction of the winding's magnetic field with a permanent magnet (or
field winding) stator. Two constants define its behavior:

- **Torque constant (Kt)**: torque = Kt Ã current.
- **Back-EMF constant (Ke)**: as the motor spins, it generates a voltage
  opposing the applied voltage, proportional to speed. In consistent SI
  units, Kt and Ke are numerically equal.

## The torque-speed curve

This is the single most important characteristic of a DC motor:

- At **zero speed** (stalled): back-EMF is zero, so current â and therefore
  torque â is at its **maximum** (stall torque). This is also when the motor
  draws maximum current and heats up fastest â stalling a motor under load
  for too long burns it out.
- At **maximum (no-load) speed**: back-EMF approximately equals supply
  voltage, current approaches zero, so torque approaches zero.
- Between these two points, torque and speed trade off **roughly linearly**.
  Picking an operating point means picking where on this line your
  application needs to sit.

## PWM speed control

DC motors are almost always speed-controlled via **PWM (Pulse Width
Modulation)**: switching the full supply voltage on and off rapidly, where
the **duty cycle** (% of time on) sets the effective average voltage seen by
the motor. This is far more power-efficient than a linear voltage regulator
(a linear approach dissipates the "wasted" voltage as heat; PWM switching
loses very little).

## Gear reduction

Motors are efficient at high speed, low torque; most robot joints need the
opposite. A **gearbox** trades speed for torque at a fixed ratio: a 10:1
gearbox roughly divides output speed by 10 and multiplies output torque by 10
(minus mechanical losses/efficiency, typically 70-90% depending on gear type).
This is why a "100 RPM, high torque" gear motor is really a fast, low-torque
motor plus a gearbox, not a fundamentally different kind of motor.

## Stall current and driver sizing

Because stall current is the highest current the motor will ever draw, motor
driver/H-bridge current ratings must be sized against **stall current**, not
typical running current â otherwise the driver can be destroyed the moment
the robot's wheel hits an obstacle and the motor stalls under load.

## Quick reference

| Condition | Torque | Current | Speed |
|---|---|---|---|
| Stalled | maximum | maximum | zero |
| No-load | ~zero | ~zero | maximum |
