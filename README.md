# Distributed algorithms in Java

Two stages of a project from [Distributed Algorithms (CS-451)](https://edu.epfl.ch/coursebook/en/distributed-algorithms-CS-451) at EPFL, taught by Prof. Rachid Guerraoui. Developed in Java on WSL2, these implementations explore communication over UDP, reliable broadcast, and agreement under simulated process crashes.

| Stage | What it implements | Source |
| --- | --- | --- |
| FIFO broadcast | UDP transport, best-effort broadcast, majority-based uniform reliable broadcast, and per-sender FIFO delivery | [`fifo-broadcast/`](fifo-broadcast/) |
| Lattice agreement | UDP transport, best-effort broadcast, and parallel single-shot lattice-agreement instances for multiple proposal slots | [`lattice-agreement/`](lattice-agreement/) |

Each stage is a separate Maven project. See the READMEs in the subdirectories for build commands and input formats.
