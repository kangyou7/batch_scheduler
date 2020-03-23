package com.glyde.mall.scheduler;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BatchSchedulerStarterTests {

	@Value("${spring.profiles.active}")
	String profile;

	@Value("#{'${job}'.replace('\n','').split(' ')}")
	private List<String> list;

	@Test
	public void contextLoads() {

		System.out.println(profile);
		list.forEach(System.out::println);

	}

}
