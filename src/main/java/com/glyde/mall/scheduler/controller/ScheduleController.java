package com.glyde.mall.scheduler.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glyde.mall.scheduler.service.DynamicScheduler;

import lombok.extern.slf4j.Slf4j;

/**
 * 스케줄 api
 * 
 * @author zeta
 *
 */

@Slf4j
@RestController
public class ScheduleController {

	@Autowired
	DynamicScheduler ds;

	static String STATUS = "status";

	/**
	 * job스케줄 중단
	 * 
	 * @param job
	 * @return
	 * @throws JsonProcessingException
	 */
	@PostMapping("/batch/stop")
	public String stop(@RequestBody String job) throws JsonProcessingException {
		@SuppressWarnings("rawtypes")
		ScheduledFuture s = ds.getJob(job);
		String result = "";

		if (s == null) {
			Map<String, String> map = new HashMap<String, String>();
			map.put(STATUS, "future-job mapping is null");
			ObjectMapper obj = new ObjectMapper();
			result = obj.writeValueAsString(map);
		} else {
			ds.cancelFuture(true, s);
			result = ds.getStatus();
		}

		log.info("/batch/stop:" + result);

		return result;
	}

	/**
	 * job스케줄 시작
	 * 
	 * @param job
	 * @return
	 * @throws JsonProcessingException
	 */
	@PostMapping("/batch/start")
	public String start(@RequestBody String job) throws JsonProcessingException {
		@SuppressWarnings("rawtypes")
		ScheduledFuture s = ds.getJob(job);
		String result = "";

		if (s == null) {
			Map<String, String> map = new HashMap<String, String>();
			map.put(STATUS, "future-job mapping is null");
			ObjectMapper obj = new ObjectMapper();
			result = obj.writeValueAsString(map);
		} else {
			ds.cancelFuture(true, s);
			ds.activateFuture(s);
			result = ds.getStatus();
		}

		log.info("/batch/start:" + result);

		return result;
	}

	/**
	 * 전체 job스케줄 중단
	 * 
	 * @return
	 * @throws JsonProcessingException
	 */
	@GetMapping("/batch/allstop")
	public String stopAll() throws JsonProcessingException {
		ds.cancelAll();
		Map<String, String> map = new HashMap<String, String>();
		map.put(STATUS, "OK");
		ObjectMapper obj = new ObjectMapper();
		String result = obj.writeValueAsString(map);

		log.info("/batch/allstop:" + result);

		return result;
	}

	/**
	 * 전체 job스케줄 시작
	 * 
	 * @param job
	 * @return
	 * @throws JsonProcessingException
	 */
	@GetMapping("/batch/allstart")
	public String startAll() throws JsonProcessingException {
		ds.activateAll();
		Map<String, String> map = new HashMap<String, String>();
		map.put(STATUS, "OK");
		ObjectMapper obj = new ObjectMapper();
		String result = obj.writeValueAsString(map);

		log.info("/batch/allstart:" + result);

		return result;
	}

}
