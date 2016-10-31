# FEC codes(RDP, STAR, Reed Solomon)

Some FEC(Forward Error Correction) codes such as RDP, Reed Solomon (P+Q), EVENODD, STAR for fault-tolerant in network communication and other conditions.

There are several known algorithms that protect data against two or more disk failures in an array of disks. Among these are
EVENODD, Reed Solomon (P+Q) erasure codes, DATUM, RDP and STAR.

RDP is most similar to EVENODD. Both EVENODD/RDP and Reed-Solomon P+Q encoding compute normal row parity for one parity disk. However, they employ different techniques for encoding the second disk of redundant data. Both use exclusive-or operations, but Reed-Solomon encoding is much more computationally intensive than EVENODD and RDP.

RDP shares many of the properties of EVENODD and Reed-Solomon encoding, in that it stores its redundant data(parity) separately on just two disks, and that data is stored in the clear on the other disks. Among the previously reported algorithms, EVENODD has the lowest computational cost for protection against two disk failures. RDP improves upon EVENODD by further reducing the computational complexity. The complexity of RDP is provably optimal, both during construction and reconstruction. 
 
I using some FEC codes such as RDP,  Reed Solomon and STAR with UDP protocol and ARQ in remote mirroring system to protect data against loss in WAN. And I did losts of testing to test this new scheme. Experimental result shows me that when packet loss probability between 0.001 and 0.1, the new scheme has better transmission performance and higher throughput than a system with TCP protocol.

So, I want to contribute these FEC codes. Because of there  algorithms are too complex, so it's too hard to find them on the net.

