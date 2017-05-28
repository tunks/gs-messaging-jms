package com.mageddo.jms.receiver;

import com.mageddo.jms.queue.DestinationConstants;
import com.mageddo.jms.queue.DestinationEnum;
import com.mageddo.jms.queue.container.BatchListMessageListenerContainer;
import com.mageddo.jms.queue.container.BatchMessage;
import com.mageddo.jms.queue.container.BatchMessageListenerContainer;
import com.mageddo.jms.service.WithdrawService;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.JMSException;

import java.util.Random;

import static com.mageddo.jms.utils.QueueUtils.configureRedelivery;
import static com.mageddo.jms.utils.QueueUtils.createContainer;

/**
 * Created by elvis on 28/05/17.
 */
@Component
public class BatchWithdrawReceiver {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private WithdrawService withdrawService;

	@Autowired
	private PlatformTransactionManager txManager;

	@Scheduled(fixedDelay = Integer.MAX_VALUE)
	public void makeWithdraws() throws JMSException {
		for(;;){
			withdrawService.createMockWithdraw();
		}
	}

	public void onMessage(final BatchMessage withdraws) throws JMSException {
		logger.info("status=onMessage, size={}", withdraws.size());
		withdrawService.doWithdraw(withdraws);
	}

	@Bean(name = DestinationConstants.WITHDRAW + "Container", initMethod = "start", destroyMethod = "stop")
	public DefaultMessageListenerContainer container(ActiveMQConnectionFactory cf, BatchWithdrawReceiver receiver){

		final DestinationEnum queue = DestinationEnum.WITHDRAW;
		configureRedelivery(cf, queue);
		final DefaultMessageListenerContainer container = createContainer(
			cf, queue.getCompleteDestination(), new BatchMessageListenerContainer(500)
		);
		container.setDestination(queue.getDestination());
		final MessageListenerAdapter adapter = new MessageListenerAdapter(receiver);
		adapter.setDefaultListenerMethod("onMessage");
		container.setMessageListener(adapter);

		return container;

	}
}