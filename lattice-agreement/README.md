# Multi-shot lattice agreement

This stage runs independent single-shot lattice-agreement instances for several proposal slots. It uses a UDP transport with batching, acknowledgements and bounded retries, plus best-effort broadcast. Binary messages carry proposals and ACK/NACK responses, and a process writes one decided set for each slot.

## Build and run

Requires Java 11 or newer and Maven. From this directory:

```bash
./build.sh
./run.sh --id 1 --hosts hosts.txt --output process1.output process1.config
```

Launch one process per host ID with the same hosts file (`id address port` on each line), a separate configuration file and a separate output path. A configuration file starts with `p vs ds` (number of slots, maximum proposal size and maximum distinct values), followed by one space-separated set of integers per slot. For example:

```text
2 3 8
1 2
2 3 4
```

The process keeps listening after writing decisions. Stop it with Ctrl+C to flush buffered output.

Key classes: [`MultiShotLA.java`](src/main/java/cs451/MultiShotLA.java), [`LatticeAgreementSingleShot.java`](src/main/java/cs451/LatticeAgreementSingleShot.java), and [`LAMessageBinary.java`](src/main/java/cs451/LAMessageBinary.java).

## Limitations

This is a course prototype. The UDP transport has bounded retransmission and does not establish formal Perfect Links guarantees under arbitrary loss. There is no Byzantine fault-tolerance claim.
