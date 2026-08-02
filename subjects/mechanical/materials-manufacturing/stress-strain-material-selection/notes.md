# Stress-Strain & Material Selection

When you're picking aluminum vs. steel vs. 3D-printed nylon for a robot
frame or bracket, you're really navigating a stress-strain curve tradeoff,
whether you draw one or not. Knowing what the curve is telling you turns
material selection from guesswork into an actual decision.

## Stress and strain, defined

- **Stress (σ)** = force / cross-sectional area. Units: Pa (N/m²), or more
  commonly MPa for engineering materials. It's "how hard the material is
  being pushed," normalized by how much material is resisting.
- **Strain (ε)** = change in length / original length. Dimensionless (or
  expressed as %). It's "how much the material actually deformed" in
  response.

## The stress-strain curve

Plot stress (y) vs. strain (x) as you load a material to failure:

- **Elastic region**: the initial, roughly straight-line portion. The
  material returns to its original shape if unloaded — no permanent
  deformation. The slope of this line is the **elastic modulus (Young's
  modulus, E)**: stiffness, independent of part geometry, a property of the
  material itself.
- **Yield strength**: the stress where the material stops behaving
  elastically and starts permanently deforming. Past this point, unloading
  the part does *not* return it to its original shape.
- **Plastic region**: continued loading past yield causes permanent
  (plastic) deformation.
- **Ultimate tensile strength**: the maximum stress the material can take
  before it starts necking down and heading toward fracture.
- **Fracture**: the material breaks.

## Why yield strength, not ultimate strength, is usually your design limit

For almost all structural robot parts, you design to stay **below yield
strength**, with a safety margin — you want the part to return to shape
after a load, not permanently bend. Designing up to ultimate strength means
the part is already permanently deformed and about to fail; that's a "this
part is destroyed" state, not a usable design margin.

## Material tradeoffs for robot frames

- **Aluminum (6061-T6 etc.)**: good strength-to-weight, easy to machine,
  reasonably corrosion resistant. The default choice for most robot
  structural parts.
- **Steel**: higher strength and stiffness than aluminum, but roughly 3x
  the density — use where stiffness-per-volume matters more than
  stiffness-per-weight, or where cost matters more than mass.
- **3D-printed plastic (PLA/PETG/nylon)**: fast iteration, low cost, low
  strength and often significant anisotropy (weaker along layer lines than
  within a layer) — good for prototypes and low-load brackets, risky for
  anything carrying real structural load without testing the actual printed
  part, not just the bulk material datasheet.

## Quick reference

| Property | Meaning |
|---|---|
| Elastic modulus (E) | stiffness; slope of the elastic region |
| Yield strength | stress where permanent deformation begins -- design limit |
| Ultimate tensile strength | max stress before fracture -- not a safe design target |
