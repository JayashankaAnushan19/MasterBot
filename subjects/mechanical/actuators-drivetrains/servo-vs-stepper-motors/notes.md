# Servo vs. Stepper Motors

Beyond plain DC motors (see DC motor fundamentals), two specialized motor
types dominate robotics wherever precise position control matters: servos
and steppers. They solve the same problem — "go to this exact position" —
in fundamentally different ways, and picking wrong shows up as either
wasted cost/complexity or missed positioning accuracy.

## Servo motors: closed-loop position control

A servo motor is a DC (or brushless) motor packaged with a position sensor
(potentiometer or encoder) and a control circuit, forming a **closed
feedback loop**: the controller continuously compares actual position
(from the sensor) to commanded position and drives the motor to close the
gap.

- **Control signal**: typically a PWM pulse where pulse *width* (not duty
  cycle in the usual sense) encodes the target position, e.g. 1-2ms pulse
  width mapping to a servo's angular range.
- **Self-correcting**: if an external force pushes the servo off target, it
  actively drives back — it *knows* where it actually is.
- **Limited range** (hobby servos): typically ~180° of rotation, though
  continuous-rotation and multi-turn servos exist.
- **Good for**: robot arm joints, camera gimbals, anywhere you need
  accurate position feedback and resistance to external disturbance.

## Stepper motors: open-loop discrete steps

A stepper motor moves in fixed, discrete angular increments ("steps," e.g.
1.8° per step = 200 steps/revolution) each time it receives a step pulse,
with no built-in position feedback.

- **Open-loop**: the controller *assumes* the motor moved exactly one step
  per pulse — it doesn't verify. If the motor stalls or is overloaded and
  **misses steps**, the controller's belief about position silently drifts
  from reality, and nothing corrects it until a re-homing (return to a known
  reference position, e.g. a limit switch).
- **Holding torque**: a stepper can hold a fixed position with real torque
  even at zero speed, without any feedback loop — useful for something like
  a 3D printer axis or CNC-style positioning where the load needs to stay
  put.
- **Microstepping**: driving a stepper with intermediate current levels
  between full steps allows smoother motion and finer effective resolution
  than the raw step angle, at some cost to torque.
- **Good for**: CNC/3D-printer-style axes, precise open-loop positioning
  where load and speed stay within the motor's step-following capability.

## The core tradeoff

| | Servo | Stepper |
|---|---|---|
| Loop | closed (feedback) | open (no feedback) |
| Knows true position? | yes | assumes, can silently be wrong |
| Behavior under missed motion | corrects | drifts, needs re-homing |
| Typical control signal | PWM pulse width | step + direction pulses |
| Common failure mode | none built-in (feedback catches it) | missed steps under overload |

## Why this matters

Choosing a stepper for a high-load axis without checking its torque curve
against the load risks silent position drift that's invisible until
something physically doesn't line up. Choosing a servo where you actually
just need to hold a fixed load position (no need to *sense* disturbance)
adds feedback-loop cost and complexity for no benefit.
