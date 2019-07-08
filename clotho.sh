#!/bin/bash
# ------------------------------------------------------------------
# [Kia Rahmani] CLOTHO
#          	Directed Test Generation for Weakly Consistent Database Systems 
# ------------------------------------------------------------------
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
# INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
# PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
# ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
# ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------



VERSION=1.0.0
USAGE="Usage: command -ihv args"

# --- Options processing -------------------------------------------
if [ $# == 0 ] ; then
    echo $USAGE
    exit 1;
fi




# FUNCTIONS
# -----------------------------------------------------------------
show () {
  echo ">> visualizing anomaly #${ANML_NO} from ${BENCHMARK}"
  ./scripts/visualizer -go analyzer/anomalies/$BENCHMARK $ANML_NO
}
# -----------------------------------------------------------------
analyze () {
  echo ">> analyzing benchmark ${BENCHMARK}"
  sed  -e 's/__BN__/'$BENCHMARK'/g' templates/config.properties > analyzer/config.properties
  ant Transformer -f analyzer/
  clean
}
# -----------------------------------------------------------------
client () {
  echo "running a client"
  cp analyzer/anomalies/$BENCHMARK/anomaly#$ANML_NO/instance.json ./tests/
  cd replayer
  mvn exec:java -Dexec.args="$CLIENT_NO"  -e 
  cd -
}
# -----------------------------------------------------------------
drive () {
  echo "running the scheduler"
  cp analyzer/anomalies/$BENCHMARK/anomaly#$ANML_NO/schedule.json ./tests/
  cd driver/target/classes
  java sync.Scheduler 100 
  cd -
}
# -----------------------------------------------------------------
setup () {
  echo "setting up the clusters and intializing them"
  . scripts/env.sh
  docker container stop $(docker ps -q)
  docker rm $(docker ps -aq)
  sleep 5
  docker run --name cas1 -p 19041:9042 -e CASSANDRA_CLUSTER_NAME=MyCluster -e CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch -e CASSANDRA_DC=DC1 -e CASSANDRA_RACK=RAC1 -d cassandra
  sleep 20
  docker run --name cas2 -p 19042:9042 -e CASSANDRA_SEEDS=172.17.0.2 -e CASSANDRA_CLUSTER_NAME=MyCluster -e CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch -e CASSANDRA_DC=DC2 -e CASSANDRA_RACK=RAC2 -d cassandra
}

# -----------------------------------------------------------------
init () {
  echo "setting up the clusters and intializing them"
  cp analyzer/src/benchmarks/$BENCHMARK/schema.cql tests
  cp analyzer/anomalies/$BENCHMARK/anomaly#$ANML_NO/$ANML_NO.cql ./tests/init.cql
  # copy and execute schema file on the Cassandra cluster
  docker cp tests/schema.cql cas1:/tmp/
  docker  exec -ti cas1 cqlsh -f "/tmp/schema.cql"
  # copy and execute intial database state 
  docker cp tests/init.cql cas1:/tmp/
  docker  exec -ti cas1 cqlsh -f "/tmp/init.cql"

}

# -----------------------------------------------------------------
clean () {
  rm analyzer/config.properties
  rm tests/*
}




# BODY 
# -----------------------------------------------------------------

BENCHMARK=$2

while [[ $# -gt 0 ]]
do
KEY="$1"

case $KEY in
    -v|--version)
    echo $VERSION
    shift # past argument
    ;;
    -a|--analyze)
    analyze
    shift # past argument
    shift # past value
    ;;
    -d|--drive)
    ANML_NO=$3
    DELAY=$4
    drive
    shift # past argument
    shift # past value
    shift # past value
    shift # past value
    ;;
    -c|--client)
    ANML_NO=$3
    CLIENT_NO=$4
    client
    shift # past argument
    shift # past value
    ;;
    -s|--setup)
    CLUSTER_SIZE=$3
    setup
    shift # past argument
    shift # past value
    ;;
    -i|--init)
    ANML_NO=$3
    init
    shift # past argument
    shift # past value
    shift # past value
    ;;
    -w|--show)
    ANML_NO=$3
    show  
    shift # past argument
    shift # past value
    shift # past value
    ;;
    -i|--cluster)
    docker exec -ti cas1 nodetool status
    shift # past argument
    ;;
    --default)
    DEFAULT=YES
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    echo $USAGE
    shift # past argument
    shift # past argument
    shift # past argument
    ;;
esac
done


































