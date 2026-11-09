package com.benedetto.game_campaign.repository;

import com.benedetto.game_campaign.entity.SpecialEventEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface SpecialEventRepository extends Repository<SpecialEventEntity, Long> {

    Optional<SpecialEventEntity> findByCode(String code);

    List<SpecialEventEntity> findByActiveTrue();
}
