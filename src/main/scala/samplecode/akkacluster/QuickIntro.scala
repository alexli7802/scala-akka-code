package samplecode.akkacluster

/* Akka-Cluster Terminology
*  - node/member: (host, port, uid), a node doesn't have to have an actor system
*  - cluster    : nodes managed by 'membership' service
*  - leader node: manage cluster 'convergence', and membership state transitions
*  - gossip protocol:
*    1. cluster-internal communication protocol.
*    2. gossip messages are serialized with 'protobuf' and 'gzipped' to reduce payload
*  - vector clocks: ???
*  - gossip convergence:
*    cluster state gets updated in each gossip convergence
*  - failure detector:
*    see if a node is 'unreachable', returns 'phi' value representing how likely a node is down.
*  - seed nodes:
*    contact points for new nodes to send 'join' commands to
* */

import com.typesafe.config.ConfigFactory

object QuickIntro {

  val cfg = ConfigFactory.parseFile( new java.io.File("conf/application.conf") )

  def main(args: Array[String]): Unit = {

    println(cfg.root().render())
  }
}
