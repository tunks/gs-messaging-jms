-- AUTOMATICALLY IMPORTED BY SPRING
-- @see https://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html

-- QUEUE CONFIGURATION TABLE
DROP TABLE DESTINATION_PARAMETER IF EXISTS ;
CREATE TABLE DESTINATION_PARAMETER (
	IDT_DESTINATION_PARAMETER INT AUTO_INCREMENT,
	NAM_DESTINATION_PARAMETER VARCHAR(255) UNIQUE,
	NUM_CONSUMERS TINYINT NOT NULL,
	NUM_MAX_CONSUMERS TINYINT NOT NULL,
	NUM_TTL INT NOT NULL,
	NUM_RETRIES TINYINT NOT NULL,
	DAT_CREATION TIMESTAMP NOT NULL,
	DAT_UPDATE TIMESTAMP NOT NULL,
	PRIMARY KEY(IDT_DESTINATION_PARAMETER)
) ;

DROP TABLE mail IF EXISTS ;
CREATE TABLE mail (
	id SERIAL,
	message VARCHAR(255)
)