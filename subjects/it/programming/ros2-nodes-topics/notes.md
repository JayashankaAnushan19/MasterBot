# ROS2 Nodes & Topics

ROS2's communication model is built on a graph of **nodes** exchanging data over
**topics**, **services**, and **actions**. Get the distinctions wrong and you'll
either block your control loop or silently drop data you needed.

## Nodes

A node is a single-purpose process (or thread, if composed) that does one job:
publish IMU data, run a planner, drive a motor controller. Nodes are discovered
and matched automatically via DDS (Data Distribution Service) discovery â no
central master, unlike ROS1's `roscore`. This is why ROS2 nodes can start in
any order and still find each other.

## Topics: async pub/sub

- **Many-to-many**: any number of publishers, any number of subscribers, on a
  named, typed topic (e.g. `/cmd_vel` of type `geometry_msgs/Twist`).
- **Fire-and-forget**: a publisher doesn't know or care if anyone is listening.
- **Best for**: continuous streams â sensor data, odometry, velocity commands.
- **QoS (Quality of Service) profiles** control delivery behavior:
  - `reliability`: `reliable` (guaranteed, retries) vs `best_effort` (drop if
    missed â use for high-rate sensor data like lidar scans).
  - `durability`: `volatile` (default, no history for late joiners) vs
    `transient_local` (late-joining subscribers get the last message â useful
    for `/map` or static config).
  - `history`/`depth`: how many past messages to buffer.
  - **Publisher and subscriber QoS must be compatible or they won't connect.**
    This is the #1 cause of "my subscriber callback never fires" bugs.

## Services: sync request/response

- One request â exactly one response, like an RPC call.
- The caller blocks (or awaits, if async client) until the response arrives.
- **Best for**: one-off actions with a clear answer â "is the gripper open?",
  "recompute this transform," "save the current map."
- Bad fit for anything long-running: a service call has no built-in way to
  report progress or be cancelled mid-flight. That's what actions are for.

## Actions: async goal-oriented tasks

- Built on topics + services under the hood: a goal request, continuous
  feedback, and a final result, plus cancellation.
- **Best for**: navigation goals, arm trajectories, anything that takes time
  and where the caller wants progress updates and the option to abort.

## Executors

A node's callbacks (topic subscriptions, service handlers, timers) don't run
themselves â an **executor** pulls them off a queue and runs them, single- or
multi-threaded. If one callback blocks (e.g. a subscription callback doing a
slow computation) on a single-threaded executor, every other callback on that
node stalls behind it. This is a common cause of "my node stopped responding
to everything" bugs that have nothing to do with the topic itself.

## Quick reference

| | Topics | Services | Actions |
|---|---|---|---|
| Pattern | pub/sub | request/response | goal/feedback/result |
| Sync? | async | sync (blocking call) | async, cancellable |
| Use for | streams | quick queries/commands | long tasks |
