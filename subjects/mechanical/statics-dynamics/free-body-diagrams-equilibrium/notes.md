# Free Body Diagrams & Equilibrium

Before you can size a bracket, check whether an arm can hold a payload
without tipping the robot, or figure out why a mount keeps failing, you need
a free body diagram (FBD). It's the single most useful habit in mechanical
analysis: isolate one object, draw every force acting on it, and nothing
else.

## What goes in a free body diagram

- The object of interest, isolated from everything around it (replace every
  connection — supports, joints, contacts — with the force/torque it
  exerts).
- Every external force: gravity (weight, acting at the center of mass),
  applied loads, normal forces from contact, tension/compression from
  cables or links, friction where relevant.
- Every external moment/torque, if the object can rotate.
- **Nothing internal**: internal forces between parts of the same isolated
  object don't appear — only what the outside world does *to* it.

Getting the isolation boundary right is most of the skill: cut through
supports/connections cleanly and replace each one with the force it exerts,
not with the object on the other side of it.

## Static equilibrium equations

For an object at rest (or moving at constant velocity — statics doesn't
require zero velocity, just zero acceleration), Newton's laws reduce to:

- **ΣF = 0**: sum of all forces in each direction (x, y, and z in 3D) is
  zero.
- **ΣΤ = 0** (sum of torques/moments about any point) is zero.

In 2D, that's 3 independent equations (ΣFx=0, ΣFy=0, ΣΤ=0) — enough to
solve for up to 3 unknowns. If your FBD has more than 3 unknown
forces/reactions in 2D, the system is **statically indeterminate** and
statics alone can't solve it (you need material/deflection info too).

## Sign conventions

Pick a consistent coordinate system and stick to it for the whole problem:
which direction is positive x, positive y, and which rotational direction
(clockwise or counterclockwise) is positive torque. Getting a sign wrong
partway through is the single most common source of wrong answers — it's
not a conceptual mistake, it's a bookkeeping one, which is exactly why
consistency matters more than which convention you pick.

## Why this matters for robotics

- **Sizing a mounting bracket**: FBD of the bracket under the load it
  carries gives you the reaction forces to design against.
- **Arm holding a payload at a given angle**: FBD of the arm (or
  isolating each joint) gives you the joint torque needed to hold position
  — directly informs motor/gearbox selection (see DC motor fundamentals:
  this is where your required stall torque number comes from).
- **Will the robot tip over**: FBD of the whole robot on the ground, taking
  moments about the tipping edge, tells you whether the center of gravity
  stays within the support base.

## Quick reference

| Step | Do this |
|---|---|
| 1 | Isolate the object; cut every connection |
| 2 | Replace each cut connection with the force/torque it applies |
| 3 | Add weight at the center of mass |
| 4 | Apply ΣF=0 (each axis) and ΣΤ=0 |
| 5 | Solve for unknowns (max 3 in 2D before indeterminate) |
