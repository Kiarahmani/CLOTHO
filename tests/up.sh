# copy and execute schema file on the Cassandra cluster
docker cp $1/schema.cql cas1:/tmp/
docker  exec -ti cas1 cqlsh -f "/tmp/schema.cql"
# copy and execute intial database state 
docker cp $1/$2/init.cql cas1:/tmp/
docker  exec -ti cas1 cqlsh -f "/tmp/init.cql"

