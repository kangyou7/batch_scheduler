package com.glyde.mall.scheduler.service;

import java.io.IOException;
import java.util.HashMap;
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
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glyde.mall.scheduler.Job;
import com.glyde.mall.scheduler.model.ConfigItem;
import com.glyde.mall.scheduler.repository.ConfigRepo;

@EnableScheduling
@Service
public class DynamicScheduler implements SchedulingConfigurer {

	private static Logger LOGGER = LoggerFactory.getLogger(DynamicScheduler.class);

	static String OK = "OK";

	Map<String, String> status = new HashMap<String, String>();
	
	@Value("${window.cmd}")
	String windowCmd;

	@Value("${linux.cmd}")
	String linuxCmd;

	ScheduledTaskRegistrar scheduledTaskRegistrar;
	Map<ScheduledFuture, Boolean> jobMap = new HashMap<>();
   
	
	/**
	 * 개발자 작성부분 - 시작
	 */
	/**
	 * 개발자 작성 - job future
	 * 배치job 개발할때 마다 추가
	 */
	ScheduledFuture samplejob;
	ScheduledFuture job2;
	ScheduledFuture job3;
	
	/**
	 * 개발자 작성 
	 * 위에 선언한 job future와 쌍으로 작성
	 * job 추가시 마다 필요한 job future가져오기
	 * 
	 * @return
	 */
	public ScheduledFuture getSampleJob() {
		return samplejob;
	}

	/**
	 * 모든 job future 취소
	 */
	public void cancelAll() {
		cancelFuture(true, samplejob);
		cancelFuture(true, job2);
		cancelFuture(true, job3);
	}

	/**
	 * 모든 job future 시작
	 */
	public void activateAll() {
		activateFuture(samplejob);
		activateFuture(job2);
		activateFuture(job3);
	}
    
	/**
	 * 개발자 작성부분 - 종료
	 */
	
	

	@Autowired
	ConfigRepo repo;

	/**
	 * initDatabase - > 테스트함수.H2에 스케줄등록
	 * 아래 예는 매 1분마다 실행
	 */
	@PostConstruct
	public void initDatabase() {
		ConfigItem config = new ConfigItem(Job.samplejob, "", "0 * * * * ?");
		repo.save(config);
	}

	@Bean
	public TaskScheduler poolScheduler() {
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
			taskRegistrar.setScheduler(poolScheduler());
		}

		if (samplejob == null || (samplejob.isCancelled() && jobMap.get(samplejob) == true)) {
			CronTrigger croneTrigger = new CronTrigger(repo.findById(Job.samplejob).get().getConfigSchedule(),
					TimeZone.getDefault());
			samplejob = taskRegistrar.getScheduler()
					.schedule(() -> scheduleCron(Job.samplejob, repo.findById(Job.samplejob).get().getConfigParam(),
							repo.findById(Job.samplejob).get().getConfigSchedule()), croneTrigger);
		}
	}

	// Only reason this method gets the cron as parameter is for debug purposes.
	public void scheduleCron(String job, String param, String cron) {
		ProcessBuilder builder = new ProcessBuilder();
		if (isWindows()) {
			LOGGER.info("===Window===");
			String springJob = "--spring.batch.job.names=" + job;
			try {
				executeShell(windowCmd, springJob, param);
				status.put(job, OK);
				LOGGER.info("No errors should be detected");
			} catch (IOException e) {
				status.put(job, e.getMessage());
				LOGGER.error(e.getMessage());
				e.printStackTrace();
			}
		} else {
			LOGGER.info("===Linux===");
			String springJob = "--spring.batch.job.names=" + job;
			try {
				executeShell(linuxCmd, springJob, param);
				status.put(job, OK);
				LOGGER.info("No errors should be detected");
			} catch (IOException e) {
				status.put(job, e.getMessage());
				LOGGER.info(e.getMessage());
				e.printStackTrace();
			}
		}

		LOGGER.info("scheduleCron: Next execution time of this taken from cron expression -> {},{}", cron, job);
	}

	/**
	 * @param mayInterruptIfRunning {@code true} if the thread executing this task
	 *                              should be interrupted; otherwise, in-progress
	 *                              tasks are allowed to complete
	 */
	public void cancelFuture(boolean mayInterruptIfRunning, ScheduledFuture future) {
		LOGGER.info("Cancelling a job schedule");

		future.cancel(mayInterruptIfRunning); // set to false if you want the running task to be completed first.
		jobMap.put(future, false);
	}

	public void activateFuture(ScheduledFuture future) {
		LOGGER.info("Re-Activating a job schedule");

		jobMap.put(future, true);
		configureTasks(scheduledTaskRegistrar);
	}

	public String getStatus() throws JsonProcessingException {
		ObjectMapper obj = new ObjectMapper();
		return obj.writeValueAsString(status);
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().startsWith("windows");
	}

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
