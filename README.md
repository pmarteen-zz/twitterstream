### Twitter Stream
A proof of concept project involving the Play Framework, Akka Streams, Twitter4j, Apache Cassandra, and Apache Spark

Inspiration taken from Introduction to Functional Programming in Scala Coursera course homework assignment involving
implementing various operations on fake tweets. I thought it would be cool to implement them on real tweets (where
applicable) :)


References:
https://goo.gl/AG1Y0i
https://loicdescotte.github.io/posts/play25-akka-streams/


## Cassandra setup
Cassandra needs to be set up and running for certain routes to work.

1. Download Cassandra from http://cassandra.apache.org/
2. Navigate to cassandra folder and run bin/cassandra to start service

You run a cqlsh (cassandra query language shell) to test what is being stored in Cassandra

The generation of keyspaces and tables will be taken care of by the actions interacting with Cassandra