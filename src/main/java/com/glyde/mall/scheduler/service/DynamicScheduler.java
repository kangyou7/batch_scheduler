package com.glyde.mall.scheduler.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glyde.mall.scheduler.model.ConfigItem;
import com.glyde.mall.scheduler.repository.ConfigRepo;

@EnableScheduling
@Service
public class DynamicScheduler implements SchedulingConfigurer {

	private static Logger log = LoggerFactory.getLogger(DynamicScheduler.class);

	static String OK = "OK";
	static String STATUS = "status";

	Map<String, String> status = new HashMap<String, String>();

	// 윈도우쉘(bat) 위치
	@Value("${window.cmd}")
	String windowCmd;

	// 리눅스쉘(sh)위치
	@Value("${linux.cmd}")
	String linuxCmd;

	ScheduledTaskRegistrar scheduledTaskRegistrar;

	@SuppressWarnings("rawtypes")
	Map<ScheduledFuture, Boolean> jobMap = new HashMap<>();

	@Value("#{'${job}'.replace('\n','').split(' ')}")
	private List<String> jobList;

	@SuppressWarnings("rawtypes")
	Map<String, ScheduledFuture> mapping = new HashMap<>();

	@Autowired
	ConfigRepo repo;

	/**
	 * initDatabase - > 테스트함수.H2에 스케줄등록 아래 예는 매 1분마다 실행
	 */
	@PostConstruct
	public void initDatabase() {

		/**
		 * job매핑 초기화
		 */
		jobList.forEach(job -> mapping.put(job, null));

		/**
		 * 테스트DB 초기화
		 */
		jobList.forEach(job -> {
			ConfigItem config = new ConfigItem(job, "", "0/10 * * * * ?");
			repo.save(config);
		});

	}

	@SuppressWarnings("rawtypes")
	public ScheduledFuture getJob(String jobName) {
		return mapping.get(jobName);
	}

	/**
	 * 모든 job future 취소
	 */
	public void cancelAll() {
		mapping.forEach((job, jobFuture) -> cancelFuture(true, jobFuture));
	}

	/**
	 * 모든 job future 시작
	 */
	public void activateAll() {
		mapping.forEach((job, jobFuture) -> activateFuture(jobFuture));
	}

	@Bean
	public TaskScheduler poolScheduler2() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("ThreadPool");
		scheduler.setPoolSize(1);
		scheduler.initialize();
		return scheduler;
	}

	// We can have multiple tasks inside the same registrar as we can see below.
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		if (scheduledTaskRegistrar == null) {
			scheduledTaskRegistrar = taskRegistrar;
		}
		if (taskRegistrar.getScheduler() == null) {
			taskRegistrar.setScheduler(poolScheduler2());
		}

		/**
		 * job 스케줄 매핑
		 */
		mapping.forEach((job, jobFuture) -> {
			if (jobFuture == null || (jobFuture.isCancelled() && jobMap.get(jobFuture) == true)) {
				CronTrigger croneTrigger = new CronTrigger(repo.findById(job).get().getConfigSchedule(),
						TimeZone.getDefault());
				jobFuture = taskRegistrar.getScheduler().schedule(() -> scheduleCron(job,
						repo.findById(job).get().getConfigParam(), repo.findById(job).get().getConfigSchedule()),
						croneTrigger);
				mapping.put(job, jobFuture);
			}
		});

	}

	// Only reason this method gets the cron as parameter is for debug purposes.
	/**
	 * 쉘 실행 함수
	 * 
	 * @param job
	 * @param param
	 * @param cron
	 */
	public void scheduleCron(String job, String param, String cron) {
		if (isWindows()) {
			log.info("===Window===");
			String springJob = "--spring.batch.job.names=" + job;
			try {
				executeShell(windowCmd, springJob, param);
				if (status.containsKey(STATUS))
					status.remove(STATUS);
				status.put(STATUS, OK);
				log.info("No errors should be detected");
			} catch (IOException e) {
				if (status.containsKey(STATUS))
					status.remove(STATUS);
				status.put(STATUS, e.getMessage());
				log.error(e.getMessage());
				e.printStackTrace();
			}
		} else {
			log.info("===Linux===");
			String springJob = "--spring.batch.job.names=" + job;
			try {
				executeShell(linuxCmd, springJob, param);
				if (status.containsKey(STATUS))
					status.remove(STATUS);
				status.put(STATUS, OK);
				log.info("No errors should be detected");
			} catch (IOException e) {
				if (status.containsKey(STATUS))
					status.remove(STATUS);
				status.put(STATUS, e.getMessage());
				log.info(e.getMessage());
				e.printStackTrace();
			}
		}

		log.info("scheduleCron: Next execution time of this taken from cron expression -> {},{}", cron, job);
	}

	/**
	 * @param mayInterruptIfRunning {@code true} if the thread executing this task
	 *                              should be interrupted; otherwise, in-progress
	 *                              tasks are allowed to complete
	 */
	/**
	 * job future 취소
	 * 
	 * @param mayInterruptIfRunning
	 * @param future
	 */
	@SuppressWarnings("rawtypes")
	public void cancelFuture(boolean mayInterruptIfRunning, ScheduledFuture future) {
		log.info("Cancelling a job schedule");

		future.cancel(mayInterruptIfRunning); // set to false if you want the running task to be completed first.
		jobMap.put(future, false);

		if (status.containsKey(STATUS))
			status.remove(STATUS);
		status.put(STATUS, OK);
	}

	/**
	 * job future 실행
	 * 
	 * @param future
	 */
	@SuppressWarnings("rawtypes")
	public void activateFuture(ScheduledFuture future) {
		log.info("Re-Activating a job schedule");

		jobMap.put(future, true);
		configureTasks(scheduledTaskRegistrar);

		if (status.containsKey(STATUS))
			status.remove(STATUS);
		status.put(STATUS, OK);
	}

	/**
	 * 처리상태 json 리턴
	 * 
	 * @return
	 * @throws JsonProcessingException
	 */
	public String getStatus() throws JsonProcessingException {
		ObjectMapper obj = new ObjectMapper();
		return obj.writeValueAsString(status);
	}

	/**
	 * 윈도우 판별
	 * 
	 * @return
	 */
	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

	/**
	 * 쉘 실행
	 * 
	 * @param shell
	 * @param springJob
	 * @param jobParam
	 * @throws ExecuteException
	 * @throws IOException
	 */
	private void executeShell(String shell, String springJob, String jobParam) throws ExecuteException, IOException {
		CommandLine cmdLine = new CommandLine(shell);
		cmdLine.addArgument(springJob);
		cmdLine.addArgument(jobParam);

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		Executor executor = new DefaultExecutor();
		executor.setExitValue(1);
		executor.execute(cmdLine, resultHandler);
	}

}
