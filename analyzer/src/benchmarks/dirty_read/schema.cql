DROP KEYSPACE IF EXISTS testks ;

create KEYSPACE testks WITH replication = {'class': 'NetworkTopologyStrategy', 'DC1':1, 'DC2':1 } ;

CREATE TABLE testks.ACCOUNTS (
    custid     BIGINT,
    name        text,
    PRIMARY KEY (custid)
);

CREATE TABLE testks.SAVINGS (
    custid      BIGINT,
    bal         BIGINT,
    PRIMARY KEY (custid)
);

CREATE TABLE testks.CHECKING (
    custid      BIGINT,
    bal         BIGINT,
    PRIMARY KEY (custid)
);

