package com.mageddo.jms.receiver;

import com.mageddo.jms.queue.DestinationConstants;
import com.mageddo.jms.service.MailService;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailReceiver {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailReceiver.class);

	@Autowired
	private MailService mailService;

	@Scheduled(fixedDelay = 10000)
	public void postMail() {
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		int qtd = 1;
		mailService.sendMockMail(qtd);
		LOGGER.info("status=success, qtd={}, time={}", qtd, stopWatch.getTime());
	}

	@JmsListener(destination = DestinationConstants.MAIL, containerFactory = DestinationConstants.MAIL + "Factory")
	public void consume(String email) throws InterruptedException {

		mailService.insert(email);
		boolean error = false;
		if (!error) {
			LOGGER.info("status=mail-received, status=success, mail={}", email);
		} else {
			LOGGER.error("status=mail-received, status=error, mail={}", email);
			throw new RuntimeException("failed");
		}
	}


}
