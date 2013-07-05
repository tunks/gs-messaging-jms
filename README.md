
Getting Started: Messaging with JMS
===================================

What you'll build
-----------------

This guide will walk you through the process of publishing and subscribing to messages using a JMS broker.

What you'll need
----------------

 - About 15 minutes
 - ActiveMQ JMS broker (instructions below)
 - A favorite text editor or IDE
 - [JDK 6][jdk] or later
 - [Maven 3.0][mvn] or later

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[mvn]: http://maven.apache.org/download.cgi

How to complete this guide
--------------------------

Like all Spring's [Getting Started guides](/getting-started), you can start from scratch and complete each step, or you can bypass basic setup steps that are already familiar to you. Either way, you end up with working code.

To **start from scratch**, move on to [Set up the project](#scratch).

To **skip the basics**, do the following:

 - [Download][zip] and unzip the source repository for this guide, or clone it using [git](/understanding/git):
`git clone https://github.com/springframework-meta/gs-messaging-jms.git`
 - cd into `gs-messaging-jms/initial`
 - Jump ahead to [Creating a message receiver](#initial).

**When you're finished**, you can check your results against the code in `gs-messaging-jms/complete`.
[zip]: https://github.com/springframework-meta/gs-messaging-jms/archive/master.zip


<a name="scratch"></a>
Set up the project
------------------

First you set up a basic build script. You can use any build system you like when building apps with Spring, but the code you need to work with [Maven](https://maven.apache.org) and [Gradle](http://gradle.org) is included here. If you're not familiar with either, refer to [Getting Started with Maven](../gs-maven/README.md) or [Getting Started with Gradle](../gs-gradle/README.md).

### Create the directory structure

In a project directory of your choosing, create the following subdirectory structure; for example, with `mkdir -p src/main/java/hello` on *nix systems:

    └── src
        └── main
            └── java
                └── hello

### Create a Maven POM

`pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>org.springframework</groupId>
	<artifactId>gs-messaging-jms-complete</artifactId>
	<version>0.1.0</version>

	<parent>
		<groupId>org.springframework.bootstrap</groupId>
		<artifactId>spring-bootstrap-starters</artifactId>
		<version>0.5.0.BUILD-SNAPSHOT</version>
	</parent>

	<dependencies>
		<dependency>
			<groupId>org.springframework.bootstrap</groupId>
			<artifactId>spring-bootstrap-web-starter</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-jms</artifactId>
		</dependency>
		<dependency>
			<groupId>org.apache.activemq</groupId>
			<artifactId>activemq-client</artifactId>
			<version>5.8.0</version>
		</dependency>
	</dependencies>

    <properties>
        <start-class>hello.Application</start-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

	<repositories>
		<repository>
			<id>spring-snapshots</id>
			<url>http://repo.springsource.org/snapshot</url>
			<snapshots><enabled>true</enabled></snapshots>
		</repository>
		<repository>
			<id>spring-releases</id>
			<url>http://repo.springsource.org/release</url>
			<snapshots><enabled>true</enabled></snapshots>
		</repository>
	</repositories>
	<pluginRepositories>
		<pluginRepository>
			<id>spring-snapshots</id>
			<url>http://repo.springsource.org/snapshot</url>
			<snapshots><enabled>true</enabled></snapshots>
		</pluginRepository>
	</pluginRepositories>
</project>
```

TODO: mention that we're using Spring Bootstrap's [_starter POMs_](../gs-bootstrap-starter) here.

Note to experienced Maven users who are unaccustomed to using an external parent project: you can take it out later, it's just there to reduce the amount of code you have to write to get started.

Installing and running ActiveMQ broker
--------------------------------------
To publish and subscribe to message, you need to install a JMS broker. For this guide, you will use ActiveMQ. Visit their [download page](http://activemq.apache.org/activemq-580-release.html), get the proper version, then unpack it.

> **Note:** ActiveMQ is an [AMQP](http://www.amqp.org/) broker that supports multiple protocols including [JMS](http://en.wikipedia.org/wiki/Java_Message_Service), the focus of this guide.

If you happen to be using a Mac with [Homebrew](http://mxcl.github.io/homebrew/), you can alternatively type:

    brew install activemq
    
On Ubuntu Linux, you can try:

    sudo apt-get install activemq
    
If you downloaded the bundle, unpack it and cd into the **bin** folder. If you used a package manager like **apt-get** or **brew**, it should already be on your path.

    activemq start
    
There are other options, but this will launch a simple broker. You should see something like this:

```
$ activemq start
INFO: Using default configuration
(you can configure options in one of these file: /etc/default/activemq /Users/gturnquist/.activemqrc)

INFO: Invoke the following command to create a configuration file
/usr/local/Cellar/activemq/5.8.0/libexec/bin/activemq setup [ /etc/default/activemq | /Users/gturnquist/.activemqrc ]

INFO: Using java '/Library/Java/JavaVirtualMachines/jdk1.7.0_11.jdk/Contents/Home//bin/java'
INFO: Starting - inspect logfiles specified in logging.properties and log4j.properties to get details
INFO: pidfile created : '/usr/local/Cellar/activemq/5.8.0/libexec/data/activemq-retina.pid' (pid '7781')
```
> **Note:** To shutdown the broker, run `activemq stop`.

Now you're all set to run the rest of the code in this guide!

<a name="initial"></a>
Creating a message receiver
---------------------------
For starters, Spring provides the means to publish messages to any POJO.

`src/main/java/hello/Receiver.java`
```java
package hello;

public class Receiver {
    public void receiveMessage(String message) {
        System.out.println("Received <" + message + ">");
    }
}
```

This is also known as a **message driven POJO**. There is no requirement on the name of the method as you'll see further below.

Sending and receiving JMS messages with Spring
----------------------------------------------
Next, you need to wire up a sender and a receiver.

`src/main/java/hello/Application.java`
```java
package hello;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

@Configuration
public class Application {
	
    static String mailboxDestination = "mailbox-destination";
	
	@Bean
	ConnectionFactory connectionFactory() {
		return new CachingConnectionFactory(
				new ActiveMQConnectionFactory("tcp://localhost:61616"));
	}
	
	@Bean
	MessageListenerAdapter receiver() {
		return new MessageListenerAdapter(new Receiver()) {{
			setDefaultListenerMethod("receiveMessage");
		}};
	}
	
	@Bean
	SimpleMessageListenerContainer container(final MessageListenerAdapter messageListener,
			final ConnectionFactory connectionFactory) {
		return new SimpleMessageListenerContainer() {{
	        setMessageListener(messageListener);
	        setConnectionFactory(connectionFactory);
	        setDestinationName(mailboxDestination);
		}};
	}
	
	@Bean
	JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}
	
    public static void main(String args[]) throws Throwable {
    	AnnotationConfigApplicationContext context = 
    			new AnnotationConfigApplicationContext(Application.class);
    	
        MessageCreator messageCreator = new MessageCreator() {
                    @Override
                    public Message createMessage(Session session) throws JMSException {
                        return session.createTextMessage("ping!");
                    }
                };
        JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
        System.out.println("Sending a new mesage.");
        jmsTemplate.send(mailboxDestination, messageCreator);
        
        context.close();
    }
}
```

The first key component is the `ConnectionFactory`. It is comprised of a `CachingConnectionFactory` wrapping an `ActiveMQConnectionFactory` pointed at `tcp://localhost:61616`, the default port of ActiveMQ.

To wrap the `Receiver` you coded earlier, use the `MessageListenerAdapter`. Then use the `setDefaultListenerMethod` to configure which method to invoke when a message comes in. This empower you to avoid implementing any JMS or broker-specific interfaces.

The `SimpleMessageListenerContainer` is an asynchronous message receiver. It uses the `MessageListenerAdapter` and the `ConnectionFactory` and is fired up when the application context starts. Another parameter is the queue name set in `mailboxDestination`.

Spring provides a convenient template class called the `JmsTemplate`. The `JmsTemplate` makes it very simple to send messages to a JMS message queue. In our `main` runner method, after starting things up, we create a `MessageCreator` and use it from our `jmsTemplate` to send a message.

> **Note:** Spring's `JmsTemplate` has the ability to receive messages, but it only works synchronously, meaning it will block. That's why it's usually recommend to use Spring's `SimpleMessageListenerCreator` with a cache-based connection factory.



Make the application executable
-------------------------------
You can bundle up the app as a runnable jar file thanks to the maven-shade-plugin as well as Spring Bootstrap's support for embedded Tomcat.

### Build an executable JAR

Now that your `Application` class is ready, you simply instruct the build system to create a single, executable jar containing everything. This makes it easy to ship, version, and deploy the service as an application throughout the development lifecycle, across different environments, and so forth.

Add the following configuration to your existing Maven POM:

`pom.xml`
```xml
    <properties>
        <start-class>hello.Application</start-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```

The `start-class` property tells Maven to create a `META-INF/MANIFEST.MF` file with a `Main-Class: hello.Application` entry. This entry enables you to run the jar with `java -jar`.

The [Maven Shade plugin][maven-shade-plugin] extracts classes from all jars on the classpath and builds a single "über-jar", which makes it more convenient to execute and transport your service.

Now run the following to produce a single executable JAR file containing all necessary dependency classes and resources:

    mvn package

[maven-shade-plugin]: https://maven.apache.org/plugins/maven-shade-plugin


Run the service
---------------
Run your service with `java -jar` at the command line:

    java -jar target/gs-messaging-jms-complete-0.1.0.jar

When it runs, you should see a couple messages:

```
Sending a new mesage.
Received <ping!>
```

Summary
-------
Congrats! You've just developed a publisher and consumer of JMS-based messages.