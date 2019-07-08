.PHONY: replayer driver analyzer

all: driver replayer analyzer


replayer: 
	cd replayer; mvn compile

driver: 
	cd driver; mvn install


analyzer:
	cd analyzer; ant



clean:
	cd replayer; mvn clean
	cd driver; mvn clean
	cd analyzer; ant clean

