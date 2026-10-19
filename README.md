# Distributed algorithms in Java

Two stages of a distributed-algorithms course project completed for EPFL's CS-451 Distributed Algorithms course. Developed and run in WSL2, the project explores communication over UDP, broadcast protocols, and agreement among crash-prone processes.

| Stage | What it implements | Source |
| --- | --- | --- |
| FIFO broadcast | UDP transport, best-effort broadcast, majority-based uniform reliable broadcast, and per-sender FIFO delivery | [`fifo-broadcast/`](fifo-broadcast/) |
| Lattice agreement | UDP transport, best-effort broadcast, and parallel single-shot lattice-agreement instances for multiple proposal slots | [`lattice-agreement/`](lattice-agreement/) |

Each stage is a separate Maven project because it has its own `cs451.Main` and evolved versions of the transport classes. See the READMEs in the subdirectories for the build commands and input formats.

The source includes adapted course scaffolding for argument parsing and building. This is a learning project rather than production infrastructure. In particular, the transport's bounded retransmission means its `PerfectLink` class does not establish formal Perfect Links guarantees under arbitrary packet loss; there is no Byzantine fault-tolerance claim. The FIFO stage provides per-sender order, not general causal order.
