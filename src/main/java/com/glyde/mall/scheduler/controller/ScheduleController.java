package com.glyde.mall.scheduler.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glyde.mall.scheduler.Job;
import com.glyde.mall.scheduler.service.DynamicScheduler;

/**
 * 스케줄 api
 * 
 * @author zeta
 *
 */
@RestController
public class ScheduleController {

	private static Logger LOGGER = LoggerFactory.getLogger(ScheduleController.class);

	@Autowired
	DynamicScheduler ds;

	/**
	 * job스케줄 중단
	 * @param job
	 * @return
	 * @throws JsonProcessingException
	 */
	@PostMapping("/batch/stop")
	public String stop(@RequestBody String job) throws JsonProcessingException {
		ScheduledFuture s = get(job);
		String result = "";

		if (s == null) {
			Map<String, String> map = new HashMap<String, String>();
			map.put(job, "future-job mapping is null");
			ObjectMapper obj = new ObjectMapper();
			result = obj.writeValueAsString(map);
		} else {
			ds.cancelFuture(true, s);
			result = ds.getStatus();
		}

		return result;
	}

	/**
	 * job스케줄 시작
	 * @param job
	 * @return
	 * @throws JsonProcessingException
	 */
	@PostMapping("/batch/start")
	public String start(@RequestBody String job) throws JsonProcessingException {
		ScheduledFuture s = get(job);
		String result = "";

		if (s == null) {
			Map<String, String> map = new HashMap<String, String>();
			map.put(job, "future-job mapping is null");
			ObjectMapper obj = new ObjectMapper();
			result = obj.writeValueAsString(map);
		} else {
			ds.cancelFuture(true, get(job));
			ds.activateFuture(get(job));
			result = ds.getStatus();
		}

		return result;
	}

	/**
	 * 전체 job스케줄 중단
	 * @return
	 * @throws JsonProcessingException
	 */
	@PostMapping("/batch/allstop")
	public String stopAll() throws JsonProcessingException {
		ds.cancelAll();
		Map<String, String> map = new HashMap<String, String>();
		map.put("allstop", "OK");
		ObjectMapper obj = new ObjectMapper();
		return obj.writeValueAsString(map);
	}

	/**
	 * 전체 job스케줄 시작
	 * @param job
	 * @return
	 * @throws JsonProcessingException
	 */
	@PostMapping("/batch/allstart")
	public String startAll(@RequestBody String job) throws JsonProcessingException {
		ds.activateAll();
		Map<String, String> map = new HashMap<String, String>();
		map.put("allstop", "OK");
		ObjectMapper obj = new ObjectMapper();
		return obj.writeValueAsString(map);
	}

	/**
	 * 개발자 작성 부분 Job과 Job future매핑
	 * 
	 * @param job
	 * @return
	 */
	private ScheduledFuture get(String job) {

		if (Job.samplejob.equals(job)) {
			LOGGER.info(job);
			return ds.getSampleJob();

		}

		return null;
	}

}
