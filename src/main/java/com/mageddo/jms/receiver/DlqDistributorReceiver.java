package com.mageddo.jms.receiver;

import com.mageddo.jms.queue.DestinationConstants;
import com.mageddo.jms.queue.DestinationEnum;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by elvis on 13/05/17.
 */
@Component
public class DlqDistributorReceiver {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private JmsTemplate jmsTemplate;

	@JmsListener(destination = DestinationConstants.DEFAULT_DLQ, containerFactory = "#{queue.get('DEFAULT_DLQ').getFactory()}")
	@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
	public void consume(ActiveMQMessage message) throws Exception {

		try {
			final ActiveMQQueue dlqQueue = getDLQ(message);
			logger.debug("status=movingDLQ, dlq={}, msgId={}, cause={}", dlqQueue.getPhysicalName(),
				message.getJMSMessageID(), message.getProperty("dlqDeliveryFailureCause"));
			jmsTemplate.convertAndSend(dlqQueue, message);
		} catch (Throwable e) {
			logger.error("errorMsg={}, msg={}", e.getMessage(), message.getJMSMessageID(), e);
			throw e;
		}

	}

	private ActiveMQQueue getDLQ(ActiveMQMessage message) throws JMSException {
		final DestinationEnum queue = DestinationEnum.fromDestinationName(message.getOriginalDestination().getPhysicalName());
		if(queue != null){
			return queue.getDlq();
		}
		return getDLQByFailureCause(message);
	}

	private ActiveMQQueue getDLQByFailureCause(ActiveMQMessage message) throws JMSException {
		final String deliveryFailureCause = message.getStringProperty("dlqDeliveryFailureCause");
		final Matcher matcher = Pattern.compile(".*destination = queue://([^,]+),.*").matcher(deliveryFailureCause);
		final ActiveMQQueue dlqQueue;
		if (matcher.find()) {
			dlqQueue = new ActiveMQQueue(matcher.group(1));
		} else {
			dlqQueue = new ActiveMQQueue("DLQ.general");
		}
		dlqQueue.setDLQ();
		return dlqQueue;
	}

}
