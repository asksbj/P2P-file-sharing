# P2P-file-sharing
In this project, I developed a P2P file transfer application similar to bittorrent. P2P(application layer architecture) communication is a distributed application architecture that partitions tasks or work loads between peers. Peers are equally privileged, equipotent participants in the application. They are said to form a peer-to-peer network of nodes. In this application, a bittorrent file sharing process is simulated. More than 10 linux machines as peers and a target file more than 1GB is tested in the project. The application is written in JAVA about 2000 lines. Bittorrent is a famous P2P protocol for file distribution. The application implement the most important feature the choking – unchoking mechanism in bittorrent. All operations are assumed to be implemented using a reliable transport protocol (socket programming). There are some protocol messages that control the bittorrent procedure. Before, transmission, the file will be divided into several pieces(each piece have about 10MB size) and each peer may have some of them and marked these pieces as already have. Each peer will have a given number of preferred neighbors which stand for the concurrent connections during data transmission. For the preferred neighbors, they will get a unchoke message from the original peer and for all other neighbors, they will get a choke. Choked neighbors can’t have connection to the peer but the chosen preferred neighbors can be changed every some intevals. There are also a kind of so called optimistically unchoke neighbors that is chosen totally randomly as the preferred neighbors for peers. Newly chosen preferred is determined by the download speed. Upon data transmission, one peer will get some file data pieces from its preferred neighbors and marked them as already have. Every time, one peer only download the pieces it doesn’t have. When a peer had all the piece which means it got the entire file, it did not ask for pieces any more but it stays at the networks in order that other peers can get pieces here. While all peers have got the file, the system shup down.
