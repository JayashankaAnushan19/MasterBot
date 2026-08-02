# Op-Amps Basics

Operational amplifiers ("op-amps") are the workhorse building block of
analog signal conditioning — turning a weak or noisy sensor signal into
something a microcontroller's ADC can actually read cleanly. You'll find
one in nearly every analog sensor front-end on a robot.

## The ideal op-amp model

Real op-amps are complex, but almost all circuit analysis starts from two
simplifying assumptions about an **ideal** op-amp:

1. **Infinite open-loop gain**: the output is (in theory) infinitely
   sensitive to the voltage difference between its two inputs, `V+` and
   `V-`. In practice this means in a stable feedback circuit, the op-amp
   drives its output to whatever value forces `V+` and `V-` to be
   (essentially) equal.
2. **No input current**: the `V+` and `V-` input pins draw (essentially)
   zero current. This is what makes op-amp circuit analysis tractable — you
   can apply Kirchhoff's current law at the input nodes without worrying
   about current flowing *into* the op-amp itself (see the Ohm's Law/KCL
   notes for the underlying rule this leans on).

These two assumptions ("virtual short" between V+/V- when there's negative
feedback, and "no input current") are the shortcut behind nearly every
op-amp circuit derivation you'll see.

## Common configurations

- **Voltage follower / buffer**: output directly feeds back to `V-`, input
  goes to `V+`. Output equals input voltage, but the op-amp's near-zero
  input current means the source driving `V+` sees almost no load — useful
  for isolating a high-impedance sensor from a downstream circuit that
  would otherwise load it down and distort the reading.
- **Non-inverting amplifier**: input to `V+`, a resistor divider from
  output back to `V-` and to ground sets the gain: `Vout = Vin × (1 +
  R2/R1)`. Output is always ≥ input in magnitude and same sign.
- **Inverting amplifier**: input through a resistor to `V-` (held near
  virtual ground via `V+` grounded), feedback resistor from output to
  `V-`. Gain is `Vout = -Vin × (Rf/Rin)` — note the sign flip and that gain
  can be set below 1 (attenuation) as well as above.
- **Comparator**: op-amp used *without* negative feedback — output slams to
  one rail or the other depending on which input (`V+` or `V-`) is higher.
  This is the one common case where the "V+ equals V-" shortcut does *not*
  apply, precisely because there's no feedback forcing them together.

## Why this matters for robotics

- **Sensor signal conditioning**: many analog sensors (thermistors, strain
  gauges, analog IR/ultrasonic rangefinders) output a small or noisy
  voltage that needs amplifying and/or buffering before a microcontroller's
  ADC can resolve it usefully.
- **Impedance isolation**: a voltage follower between a high-impedance
  sensor and a lower-impedance ADC input prevents the ADC's loading from
  distorting the sensor reading.

## Quick reference

| Configuration | Feedback? | Output |
|---|---|---|
| Voltage follower | yes | Vout = Vin, high input impedance |
| Non-inverting amp | yes | Vout = Vin × (1 + R2/R1) |
| Inverting amp | yes | Vout = -Vin × (Rf/Rin) |
| Comparator | no | slams to one supply rail based on which input is higher |
