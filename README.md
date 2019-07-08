### CLOTHO:  Directed Test Generation for Weakly Consistent Database Systems
description of the tool

dependencies


#### Setup
``` sh
git clone https://github.com/Kiarahmani/CLOTHO.git
```
Move to the directory `cd CLOTHO` and run the following command:
``` sh
./clotho.sh --setup 2
```
You can verify that the cluster is correctly set up by running: 
```sh 
./clotho.sh --cluster
```


#### Static Analysis 
You should choose a _benchmark_name_ from pre-defined examples {dirty_read, dirty_write} or implement your own (more information below).
Following command will comiple the source codes, including the application under test:
``` sh
make benchmark=_benchmark_name_
```
Now you can run CLOTHO's static analyzer by the following command (you can optionlly configure the analysis by editing `templates/config.properties`).
``` sh
./clotho.sh --analyze _benchmark_name_
```
Once the analysis finishes, CLOTHO will report the number of static anomalies found in _benchmark_name_:
....
You can view anomaly _anomaly_number_ by running: 
./clotho.sh --show _benchmark_name_ _anomaly_number_

#### Replaying Anomalies
Following command will create proper keyspace and required schema on the Cassandra cluster:
```sh
./clotho.sh --init _benchmark_name_ _anomaly_number_
```
Now open separate terminals: one for the central test controller and one for each transaction (aka client) involned in the anomaly.
Run  the following command in the controller terminal:
```sh
./clotho.sh -d _benchmark_name_ _anomaly_number_
```
Finally, run the following command at each client terminal:
```sh
./clotho.sh --client _benchmark_name_ _anomaly_number_ _client_number_
```
