FROM anapsix/alpine-java

ADD ./target/scala-2.12/rasterdata-endless-source.jar  /rasterdata-endless-source.jar

WORKDIR /

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /rasterdata-endless-source.jar"]
