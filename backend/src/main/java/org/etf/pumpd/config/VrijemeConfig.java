package org.etf.pumpd.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

@Configuration
public class VrijemeConfig {

	@Bean
	@ConditionalOnMissingBean
	public Clock clock()
	{
		return Clock.systemDefaultZone();
	}
}
