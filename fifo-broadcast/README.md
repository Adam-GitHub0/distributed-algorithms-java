# Uniform reliable and FIFO broadcast

This stage implements a UDP transport, best-effort broadcast, majority-based uniform reliable broadcast (URB), and a per-sender FIFO delivery layer in Java. Each process broadcasts numbered messages, tracks which processes forwarded a message, then buffers delivered messages until preceding messages from the same original sender have been delivered.

## Build and run

Requires Java 11 or newer and Maven. From this directory:

```bash
./build.sh
./run.sh --id 1 --hosts hosts.txt --output process1.output process1.config
```

Launch one process per host ID with the same hosts file (`id address port` on each line) and a separate configuration file containing the number of messages that process should broadcast. For example, a configuration file with `10` makes that process broadcast the values `1` through `10`. Each process writes `b sequence` lines for broadcasts and `d sender sequence` lines for deliveries. Stop the process with Ctrl+C to flush buffered output.

Key classes: [`UniformReliableBroadcast.java`](src/main/java/cs451/UniformReliableBroadcast.java), [`FIFOBroadcast.java`](src/main/java/cs451/FIFOBroadcast.java), and [`UnicastSender.java`](src/main/java/cs451/UnicastSender.java).

## Limitations

The sender uses a fixed retry limit, so this implementation does not guarantee delivery under arbitrary packet loss despite the `PerfectLink` class name. Its FIFO property concerns messages from the same original sender; it does not establish causal ordering between different senders. This is a course prototype, not a hardened network service.
