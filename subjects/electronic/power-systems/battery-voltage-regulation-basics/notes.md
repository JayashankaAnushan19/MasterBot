# Battery & Voltage Regulation Basics

Every mobile robot's electronics live downstream of a battery whose voltage
sags under load and drifts as it discharges — and every one of those
electronics expects a stable voltage. The gap between "what the battery
actually outputs" and "what the circuit needs" is voltage regulation's job,
and getting it wrong is one of the most common causes of mysterious robot
resets and brownouts.

## LiPo basics

- A single lithium-polymer cell is nominally **3.7V**, fully charged
  ~4.2V, and should not be discharged below ~3.0V (over-discharge damages
  the cell and is a fire/safety risk).
- **Series (S) configuration** adds cell voltages: a 3S pack is ~11.1V
  nominal (3 × 3.7V), ~12.6V fully charged.
- **Parallel (P) configuration** adds capacity (mAh) at the same voltage: a
  2P pack has double the capacity of a single cell at the same voltage.
- A "3S2P" pack: 3 cells in series (voltage) × that group doubled in
  parallel (capacity).

## Why voltage regulation is necessary

A battery's voltage isn't constant — it's highest when full, drops as it
discharges, and **sags further under load** (higher current draw pulls
voltage down temporarily, worse as internal resistance rises with age/cold).
Most electronics (microcontrollers, sensors, logic ICs) need a stable,
specific voltage (commonly 5V or 3.3V) regardless of what the battery is
actually doing. A **voltage regulator** sits between battery and load to
provide that stability.

## Regulator types

- **LDO (low-dropout linear regulator)**: simple, cheap, low noise — but
  inefficient. It dissipates the voltage difference as heat
  (`P_wasted = (V_in - V_out) × I`), so it's a poor choice when the
  input/output voltage gap or current draw is large (compare to the PWM
  motor-control efficiency argument in the DC motor notes — same principle).
- **Buck (step-down switching) converter**: steps voltage down efficiently
  by switching, not dissipating — the right choice for higher-current loads
  or a large voltage gap (e.g. regulating a 3S LiPo down to 5V for logic).
- **Boost (step-up switching) converter**: steps voltage *up* — needed when
  the battery voltage is lower than what a load requires (e.g. a single
  LiPo cell at 3.7V boosted to 5V).

## Brownouts under motor stall current

Recall from DC motor fundamentals: stall current is the highest current a
motor draws. If several motors stall simultaneously (e.g. the robot hits an
obstacle), the sudden current spike can sag the battery/rail voltage enough
to reset a microcontroller or brown out sensors — even though the same
regulator worked fine under normal load. This is why current budgeting
(section: DC motor notes' driver-sizing discussion) and regulator headroom
both matter, not just steady-state numbers.

## Low-voltage cutoff

LiPo packs need a **low-voltage cutoff** (in the ESC/BMS or a dedicated
monitor) that stops discharge before cell voltage drops below the safe
~3.0V floor — over-discharging a LiPo isn't just "reduced capacity," it's
permanent cell damage and a safety hazard.

## Quick reference

| Regulator | Efficiency | Use when |
|---|---|---|
| LDO (linear) | low (dissipates heat) | small voltage gap, low current |
| Buck (step-down) | high | stepping voltage down, higher current |
| Boost (step-up) | high | output voltage higher than input |
