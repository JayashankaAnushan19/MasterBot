# Forward & Inverse Kinematics

Every robot arm needs to answer two opposite questions: "given these joint
angles, where is my end effector?" (forward kinematics) and "to reach this
point, what joint angles do I need?" (inverse kinematics). FK is
straightforward algebra. IK is where the real engineering is.

## Forward Kinematics (FK)

- Input: joint angles (or displacements, for prismatic joints). Output:
  end-effector pose (position + orientation) in the world/base frame.
- Computed by chaining transformation matrices, one per joint, from base to
  end effector: `T_end = T_1 · T_2 · ... · T_n`.
- **Denavit-Hartenberg (DH) parameters** are the standard convention for
  defining each joint's transform with just 4 numbers per joint: link length
  `a`, link twist `Î±`, link offset `d`, joint angle `Î¸`. This standardization
  is what lets FK be expressed generically for any serial-chain manipulator.
- FK always has exactly **one solution** for a given set of joint values â
  it's just matrix multiplication, no ambiguity.

## Inverse Kinematics (IK)

- Input: desired end-effector pose. Output: joint angles that achieve it.
- **Not always solvable in closed form**, and when a solution exists it's
  often **not unique** â a 6-DOF arm reaching a point can frequently do so in
  multiple elbow-up/elbow-down configurations.
- Three broad solving approaches:
  1. **Analytical/closed-form**: solve the geometry directly (e.g. law of
     cosines for a 2-link planar arm). Fast, exact, but only works for
     specific, often simpler, arm geometries.
  2. **Numerical/iterative**: start from a guess, use the **Jacobian**
     (relates joint velocity to end-effector velocity) to iteratively step
     joint angles toward the target, e.g. Jacobian transpose or
     pseudo-inverse methods. Works generically but can converge slowly or hit
     singularities.
  3. **Optimization-based**: frame IK as minimizing pose error subject to
     joint limits/collision constraints â common in modern motion-planning
     stacks (e.g. MoveIt's IK solvers, TRAC-IK).

## Singularities

A **kinematic singularity** is a configuration where the Jacobian loses rank
â the arm loses a degree of freedom of motion in some direction (e.g. an arm
fully extended can't push further outward, only rotate). Near singularities,
small end-effector velocity demands can require huge, sometimes unachievable
joint velocities. IK solvers must detect and handle these (e.g. damped
least-squares) rather than blindly inverting the Jacobian.

## Degrees of freedom (DOF)

To reach an arbitrary position **and** orientation in 3D space you need
**6 DOF** (3 translation + 3 rotation). Arms with fewer DOF (e.g. 4-DOF) can
only reach a subset of poses; arms with more than 6 DOF are **redundant** â
multiple joint configurations solve the same end-effector pose, which IK
solvers can exploit to additionally avoid obstacles or joint limits.

## Quick reference

| | Forward Kinematics | Inverse Kinematics |
|---|---|---|
| Input | joint angles | end-effector pose |
| Output | end-effector pose | joint angles |
| Solutions | always exactly one | zero, one, or many |
| Difficulty | direct computation | often iterative/numerical |
