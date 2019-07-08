DROP KEYSPACE IF EXISTS testks ;

create KEYSPACE testks WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1':1, 'DC2':1 } ;

CREATE TABLE testks.ACCOUNTS (
    id 	         BIGINT,
    value        BIGINT,
    PRIMARY KEY (id)
);


