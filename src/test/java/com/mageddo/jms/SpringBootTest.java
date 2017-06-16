package com.mageddo.jms;

import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by elvis on 16/06/17.
 */
@org.springframework.boot.test.context.SpringBootTest
@ContextConfiguration(classes = {ApplicationTest.class}, loader = SpringBootContextLoader.class)
public @interface SpringBootTest {
}
