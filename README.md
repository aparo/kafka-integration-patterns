This is a little demo project to show how a combination of a [akka-streams](http://www.lightbend.com/activator/template/akka-stream-scala),
 [Reactive Kafka](https://github.com/akka/reactive-kafka), [Kafka](https://kafka.apache.org/) and ElasticSearch can be used as the ingestion part
 of a fast data system.

#Setup
Docker configuration: add in share:

    /opt/data
    /opt/dockerdata
    <your source dir>

Download the geoname sample:

    ./fetch_geoname.sh

Setup Prometheus/Grafana and family:

    cd docker/dockprom
    docker-compose up -d
    
Setup ElasticSearch/Kibana/kafka:

    cd docker/kafka_elk/
    docker-compose up -d

Build the apps:

    sbt
    project csvToKafka
    docker:publishLocal
    project kafkaToES
    docker:publishLocal
    
Add the new deployed apps:

    docker-compose up -d

Enjoy:

- Prometheus: http://0.0.0.0:9090 (admin:admin)
- Grafana: http://0.0.0.0:3000 (admin:admin)
- Kafka-Manager: http://0.0.0.0:9000
- Kibana: http://0.0.0.0:5601 
- Cerebro: http://0.0.0.0:9001 

