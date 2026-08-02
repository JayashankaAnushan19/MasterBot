# Dynamic Programming Basics

Dynamic programming (DP) is what you reach for when a brute-force recursive
solution re-solves the same subproblem thousands of times. It's not a
different algorithm family so much as a discipline: recognize the repeated
work, cache it, stop paying for it twice.

## The two required properties

DP only applies when a problem has both:

- **Overlapping subproblems**: naive recursion calls the same subproblem
  multiple times (classic example: naive recursive Fibonacci recomputes
  `fib(2)` exponentially many times).
- **Optimal substructure**: the optimal solution to the whole problem can be
  built from optimal solutions to its subproblems.

If either is missing, DP either doesn't help (no overlap, nothing to cache)
or doesn't apply (no optimal substructure, a greedy local choice doesn't
compose into a global optimum).

## Memoization vs. tabulation

- **Memoization (top-down)**: keep the natural recursive structure, but
  cache each subproblem's result in a map/array the first time it's
  computed, and return the cached value on repeat calls. Easiest to write by
  starting from a brute-force recursive solution and adding a cache.
- **Tabulation (bottom-up)**: build a table iteratively from the smallest
  subproblems up to the full problem, no recursion. Usually faster in
  practice (no call-stack overhead) and lets you reason directly about space
  optimization (often you only need the last row/few entries, not the whole
  table).

## Why this matters for robotics

- **Discretized path planning**: grid-based planners (value iteration,
  Dijkstra-style cost propagation) are DP in disguise — the cost-to-go from
  a cell depends on the optimal cost-to-go of its neighbors, computed once
  and reused.
- **Trajectory optimization**: some motion-planning formulations break a
  trajectory into stages and solve backward from the goal, caching the
  optimal cost-to-go at each stage — this is DP applied to continuous state
  spaces (discretized).

## Complexity payoff

A naive exponential recursion (e.g. O(2^n) for unoptimized Fibonacci)
collapses to O(n) time with O(n) (or O(1) with space optimization) once
overlapping calls are cached. The general pattern: **# of distinct
subproblems × cost to compute each one**, instead of exponential re-work.

## Quick reference

| | Memoization | Tabulation |
|---|---|---|
| Direction | top-down (recursive) | bottom-up (iterative) |
| Easiest to derive from | brute-force recursion + cache | subproblem dependency order |
| Overhead | recursion/call stack | none |
| Space optimization | harder to see | easier (often just keep last row) |
