
package com.mageddo.jms;

import com.mageddo.jms.config.MageddoMessageListenerContainerFactory;
import com.mageddo.jms.queue.CompleteDestination;
import com.mageddo.jms.queue.DestinationEnum;
import com.mageddo.jms.service.DestinationParameterService;
import com.mageddo.jms.utils.QueueUtils;
import com.mageddo.jms.vo.Color;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.jms.Session;
import java.util.Arrays;
import java.util.concurrent.Executors;

@EnableScheduling
@EnableTransactionManagement
@EnableJms
@EnableAsync
@EnableAspectJAutoProxy

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
public class Application implements SchedulingConfigurer {

	@Autowired
	ActiveMQConnectionFactory activeMQConnectionFactory;

	@Autowired
	ConfigurableBeanFactory beanFactory;

	@Autowired
	DefaultJmsListenerContainerFactoryConfigurer configurer;

	@Autowired
	DestinationParameterService destinationParameterService;

	@PostConstruct
	public void setupQueues(){

		activeMQConnectionFactory.setTrustedPackages(Arrays.asList(Color.class.getPackage().getName()));
		for (final DestinationEnum destinationEnum : DestinationEnum.values()) {

			if(destinationEnum.isAutoDeclare()){
				declareQueue(destinationEnum, activeMQConnectionFactory, activeMQConnectionFactory, beanFactory, configurer);
			}
			destinationParameterService.createDestinationParameterIfNotExists(destinationEnum.getCompleteDestination());

		}

	}

	private MageddoMessageListenerContainerFactory declareQueue(
			DestinationEnum destinationEnum,
			ActiveMQConnectionFactory activeMQConnectionFactory, ConnectionFactory cf,
			ConfigurableBeanFactory beanFactory, DefaultJmsListenerContainerFactoryConfigurer configurer
	) {

		final CompleteDestination destination = destinationEnum.getCompleteDestination();

		final MageddoMessageListenerContainerFactory factory = QueueUtils.createDefaultFactory(
			activeMQConnectionFactory, destination
		);
		QueueUtils.configureRedelivery(activeMQConnectionFactory, destinationEnum);
		configurer.configure(factory, cf);
		beanFactory.registerSingleton(factory.getBeanName(), factory);
		return factory;
	}


	@Primary
	@Bean
	@ConfigurationProperties(prefix = "spring.activemq.pool")
	public PooledConnectionFactory pooledConnectionFactory(ActiveMQConnectionFactory activeMQConnectionFactory){

		final PooledConnectionFactory cf = new PooledConnectionFactory();
		cf.setConnectionFactory(activeMQConnectionFactory);
		return cf;
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.activemq")
	public ActiveMQConnectionFactory activeMQConnectionFactory(ActiveMQProperties properties){
		final ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(
			properties.getUser(), properties.getPassword(), properties.getBrokerUrl()
		);
		if(properties.getPackages().getTrustAll()){
			cf.setTrustAllPackages(true);
		}
		cf.setUseAsyncSend(true);
		cf.setDispatchAsync(true);
		cf.setUseCompression(true);
		return cf;
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.activemq")
	public ActiveMQProperties activeMQProperties(){
		return new ActiveMQProperties();
	}

	@Primary
	@Bean
	public JmsTemplate jmsTemplate(PooledConnectionFactory connectionFactory){
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
		jmsTemplate.setSessionTransacted(true);
		return jmsTemplate;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(Executors.newScheduledThreadPool(50));
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
