# TCP vs UDP Fundamentals

Every network message a robot sends — telemetry, control commands, sensor
streams, log uploads — rides on one of two transport protocols. Picking the
wrong one is a common source of "why is my teleoperation laggy" or "why did
I silently lose sensor frames" bugs.

## TCP: reliable, ordered, connection-based

- **Handshake required**: a 3-way handshake sets up a connection before any
  data flows, and the connection is torn down explicitly when done.
- **Guarantees**: every byte arrives, in order, or the connection reports an
  error. Lost packets are automatically retransmitted.
- **Cost of that guarantee**: retransmission and in-order delivery mean a
  single lost packet can stall delivery of everything behind it
  ("head-of-line blocking") until it's resent — bad for latency-sensitive
  real-time data.
- **Good for**: firmware updates, log/file uploads, anything where
  correctness matters more than latency and a stall is acceptable.

## UDP: fast, unordered, connectionless

- **No handshake, no guarantee**: packets ("datagrams") are just sent; there
  is no built-in acknowledgment, retransmission, or ordering.
- **Lower latency, lower overhead**: no connection setup, no head-of-line
  blocking — a lost packet is just gone, and the next one arrives on
  schedule regardless.
- **Good for**: continuous, high-rate streams where the *next* value matters
  more than guaranteeing every past value arrived — sensor telemetry, video,
  real-time control commands. Losing one IMU reading out of a 200Hz stream
  is fine; stalling the whole stream waiting for a retransmit is not.

## Why this matters for ROS2 specifically

ROS2's DDS middleware (see the ROS2 nodes & topics notes) is built on UDP by
default. That's exactly why QoS profiles exist at the ROS2 layer: DDS
reimplements *selective* reliability on top of UDP when you actually need
it (`reliability: reliable`), while still allowing `best_effort` for
high-rate sensor topics where occasional loss is fine and low latency
matters more. This is the same TCP-vs-UDP tradeoff, just made configurable
per-topic instead of being an all-or-nothing transport choice.

## Ports

Both protocols use 16-bit port numbers (0-65535) to route traffic to the
right application on a host; a TCP port and a UDP port with the same number
are independent — a service listening on TCP port 8080 does not conflict
with one listening on UDP port 8080.

## Quick reference

| | TCP | UDP |
|---|---|---|
| Connection | handshake required | none |
| Delivery guarantee | yes, ordered, retransmitted | no |
| Latency | higher (retransmit stalls) | lower |
| Good for | firmware updates, logs | telemetry, video, real-time control |
