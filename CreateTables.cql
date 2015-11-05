DROP KEYSPACE IF EXISTS demo;
CREATE KEYSPACE demo WITH REPLICATION = { 'class':'SimpleStrategy', 'replication_factor':1};

CREATE TABLE demo.data (
	edge_id text,
	sensor text,
	epoch_hr text,
	ts timestamp,
	depth double,
	value double,
	PRIMARY KEY (( edge_id, sensor, epoch_hr ), ts)
) WITH default_time_to_live = 180;

CREATE TABLE demo.last (
	edge_id text,
	sensor text,
	ts timestamp,
    depth double,
	value double,
	PRIMARY KEY (( edge_id, sensor ))
) WITH default_time_to_live = 180;

CREATE TABLE demo.count (
	pk int,
	ts timestamp,
	count bigint,
	count_ma double,
	PRIMARY KEY (pk, ts)
) WITH default_time_to_live = 180;