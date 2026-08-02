# Graph Search: BFS & DFS

Path planning, occupancy-grid exploration, and dependency resolution in build
systems all reduce to the same primitive: searching a graph. BFS and DFS are
the two base traversal strategies everything else (Dijkstra, A*, topological
sort) builds on.

## Breadth-First Search (BFS)

- Explores the graph **layer by layer** from the start node, using a **FIFO
  queue**.
- Guarantees the **shortest path in terms of edge count** on an unweighted
  graph â this is the key property that makes it useful, not just "explores
  wide."
- Space cost can be steep: the frontier (queue) can hold O(b^d) nodes for
  branching factor `b` and depth `d`.
- In robotics: BFS over an occupancy grid gives you the shortest unweighted
  path in grid cells, useful as a cheap first-pass planner or for computing
  distance transforms.

## Depth-First Search (DFS)

- Explores as far as possible down one branch before backtracking, using a
  **stack** (explicit or via recursion).
- Does **not** guarantee shortest path â it just finds *a* path.
- Much lower memory footprint than BFS: O(d) for depth `d`, not O(b^d).
- Natural fit for: cycle detection, topological sort (dependency graphs,
  e.g. resolving node startup order), connected-component labeling.

## Complexity

Both are **O(V + E)** â every vertex and edge visited once, given an adjacency
list. The practical difference is memory (frontier size), not asymptotic time.

## Choosing between them

| Need | Use |
|---|---|
| Shortest path, unweighted graph | BFS |
| Any valid path, low memory budget | DFS |
| Cycle detection / topological order | DFS |
| Exploring nearby space first (e.g. frontier exploration in SLAM) | BFS |

## Beyond BFS/DFS

Neither accounts for edge weights or cost. When cells/edges have varying cost
(rough terrain, distance, energy), you move to **Dijkstra** (weighted BFS,
priority queue instead of FIFO) or **A\*** (Dijkstra + a heuristic estimate of
remaining distance to goal, which is what makes it usually faster in practice
for point-to-point robot path planning).
