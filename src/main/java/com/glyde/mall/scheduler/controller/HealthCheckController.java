package com.glyde.mall.scheduler.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 헬스체크
 * 
 * @author zeta
 *
 */
@RestController
public class HealthCheckController {

	private static Logger log = LoggerFactory.getLogger(HealthCheckController.class);

	@GetMapping("/system/healthcheck")
	public String healthcheck() throws JsonProcessingException {

		Map<String, String> map = new HashMap<String, String>();
		map.put("status", "OK");
		ObjectMapper obj = new ObjectMapper();
		String result = obj.writeValueAsString(map);

		log.info("/system/healthcheck:" + result);

		return result;
	}

}
