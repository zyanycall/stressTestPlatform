FROM registry.docker.yit.com/official/openjdk:8u162-jdk-stretch
EXPOSE 8080

VOLUME /tmp
ENV PROJECT_PATH="/stresstest"
ENV JMETER_VERSION="apache-jmeter-5.1.1"
ENV JMETER_HOME="$PROJECT_PATH/apache-jmeter"

RUN mkdir -p $PROJECT_PATH/config \
	&& wget -qO- https://archive.apache.org/dist/jmeter/binaries/$JMETER_VERSION.tgz | tar zxf - -C $PROJECT_PATH \
	&& mv $PROJECT_PATH/$JMETER_VERSION $JMETER_HOME \
	&& echo "\nserver.rmi.ssl.disable=true" >> $JMETER_HOME/bin/jmeter.properties

ENV PATH="${JMETER_HOME}/bin/:${PATH}"

VOLUME $PROJECT_PATH/config
