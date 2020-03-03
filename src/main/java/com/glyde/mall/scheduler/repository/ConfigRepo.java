package com.glyde.mall.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glyde.mall.scheduler.model.ConfigItem;

public interface ConfigRepo extends JpaRepository<ConfigItem, String> {
}
