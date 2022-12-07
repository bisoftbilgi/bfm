package com.bisoft.bfm.dto;

public enum DatabaseStatus {

    MASTER_WITH_NO_SLAVE, //
    //	INIT, // BFM server is starting up and doesn't know the node's position yet....
    NO_REPLICATION, // if the replication didn't established BFM can't do anything for this version.
    // for future maybe we may do something else like loose couple DBs
    SHOTDOWN, // BFM node is in a shutdown process

    MASTER, // BFM Node is Master now
    SLAVE, // BFM Node is Slave now
    SLAVE_WITH_SLAVE,
    TIMEOUT, // The database is not responding in an acceptable period of time
    INACCESSIBLE,
    // The database is not accessable and throwing exception during the attempt
    // period
    UNKNOWN, // State is unknown...
}