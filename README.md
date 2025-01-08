# Renos API

Java reference implementation and test tool.

## Working directory is java, so go to it
	cd java
	
## To build the code
	mvn clean install

## To run the code
	java -jar target/websocket-client-example-1.2-jar-with-dependencies.jar http://<unit ip address>

# API dependency
The example code depends on an API library `renos-api-message`. This API library is provided within the `java/repo` directory, which is a file-based maven repository.

## Include the library in your Maven project
To include the API library in your project, make sure the dependency is available in some repository and reference the repository in your `pom.xml`:

    <project>
        <repositories>
            <repository>
                <id>project.local</id>
                <name>project</name>
                <url>file:${project.basedir}/repo</url>
            </repository>
        </repositories>
    </project>

Then add the dependency:

    <project>
        <dependencies>
            <dependency>
                <groupId>com.nedap.retail.renos.api.v2</groupId>
                <artifactId>renos-api-message</artifactId>
                <version>1.0</version>
            </dependency>
        </dependencies>
    </project>



