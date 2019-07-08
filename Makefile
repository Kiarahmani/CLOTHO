.PHONY: replayer driver analyzer

all: init driver replayer analyzer 


init: 
	rm -rf ./tests
	mkdir tests


script: 
	cp scripts/clotho.sh ./
	chmod +x clotho.sh


replayer:
	cp replayer/src/main/java/com/github/kiarahmani/replayer/client_templates/$(benchmark).templatec replayer/src/main/java/com/github/kiarahmani/replayer/Client.java
	cd replayer; mvn compile

driver: 
	cd driver; mvn install


analyzer:
	cd analyzer; ant



clean:
	cd replayer; mvn clean
	cd driver; mvn clean
	cd analyzer; ant clean
	rm -rf ./tests

