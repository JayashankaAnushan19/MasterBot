# Ohm's Law & Kirchhoff's Laws

Three equations underlie almost every circuit-debugging session you'll ever
do: Ohm's Law and Kirchhoff's two laws. Fluency here means you can read a
schematic and reason about it without a simulator.

## Ohm's Law

**V = I Ã R** â voltage across a resistive element equals current through it
times its resistance. Trivial to state, easy to misapply: it only directly
relates V, I, R for a single resistive element, not a whole circuit at once.
For a whole circuit you combine it with Kirchhoff's laws below.

Power dissipated: **P = I Ã V = IÂ²R = VÂ²/R** â three equivalent forms, pick
whichever variables you already know. The IÂ²R form is why doubling current
through a resistor quadruples its heat dissipation, not just doubles it.

## Kirchhoff's Current Law (KCL)

**The sum of currents into a node equals the sum of currents out of it.**
Charge doesn't accumulate at a junction. Practically: at any point where
wires meet, add up what's flowing in, and it must equal what's flowing out.
This is what lets you solve for an unknown branch current if you know the
others at a node.

## Kirchhoff's Voltage Law (KVL)

**The sum of voltage rises and drops around any closed loop is zero.**
Walk around any loop in a circuit, add voltage gains (e.g. across a battery,
in the direction of travel) and subtract voltage drops (e.g. across a
resistor), and it sums to zero. This is what lets you solve for an unknown
voltage drop across one component if you know the others in its loop.

## Series vs. parallel

- **Series resistors**: same current flows through all of them (nowhere else
  for it to go). Total resistance: **R_total = R1 + R2 + ...** â always
  larger than any individual resistor.
- **Parallel resistors**: same voltage across all of them (same two nodes).
  Total resistance: **1/R_total = 1/R1 + 1/R2 + ...** â always smaller than
  the smallest individual resistor. For exactly two resistors this simplifies
  to `R_total = (R1Ã R2)/(R1+R2)`.

## Why this matters for robotics electronics

- Sizing a **current-limiting resistor** for an LED or sensor: Ohm's Law
  directly, `R = (V_supply - V_component) / I_desired`.
  - **Voltage divider** circuits (common for scaling a sensor signal down to
  an ADC's input range) are a direct KVL + series-resistance application:
  `V_out = V_in Ã R2/(R1+R2)`.
- Debugging "why is this rail sagging under load": KCL tells you every amp
  drawn by every downstream component has to be supplied from somewhere â
  add up the current budget before blaming the regulator.

## Quick reference

| Law | Statement | Use it to... |
|---|---|---|
| Ohm's Law | V = IR | relate V/I/R for one element |
| KCL | Î£ I_in = Î£ I_out at a node | solve unknown branch currents |
| KVL | Î£ V around a closed loop = 0 | solve unknown voltage drops |
