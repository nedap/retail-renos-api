# Renos API

Java reference implementation and test tool.

## Working directory is java, so go to it
	cd java
	
## To build the code
	mvn clean install

## To run the code
	java -jar target/websocket-client-example-1.0-jar-with-dependencies.jar http://<unit ip address>

# Api Updates
	If the implementation of the api-v2 is changed at renos, it must be added to this reference implementation also

## Update the repo
	cd ~/workspace/renos/retail-renos/api-v2-messages
	nano pom.xml
		add just before </project>
		
		    <distributionManagement>
			<repository>
			    <id>examplecode</id>
			    <name>Example code repo</name>
			    <url>file://${project.basedir}/../../retail-renos-api/java/repo</url>
			</repository>
		    </distributionManagement>

	mvn deploy
	
	and then rebuild the code as described before



