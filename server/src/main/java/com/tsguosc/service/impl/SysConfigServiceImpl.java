package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.ConfigKeys;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.ConfigUpdateRequest;
import com.tsguosc.dto.ConfigVO;
import com.tsguosc.dto.RecruitInfoVO;
import com.tsguosc.entity.SysConfig;
import com.tsguosc.mapper.SysConfigMapper;
import com.tsguosc.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置实现。
 *
 * <p>不做缓存：配置读取频率低、改完要立即生效（报名开关关掉就得马上生效）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    /** 报名开关默认值：默认开放（首次部署时种子里也会写入 1） */
    private static final String DEFAULT_RECRUIT_OPEN = "1";

    private static final String DEFAULT_REVIEW_NOTICE = "我们会尽快完成审核，请留意查询页的状态更新。";
    private static final String DEFAULT_CLUB_INTRO = "开源鸿蒙社欢迎你。";

    private final SysConfigMapper sysConfigMapper;

    @Override
    public Optional<String> find(String key) {
        SysConfig entity = sysConfigMapper.selectOne(
                Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, key).last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.ofNullable(entity.getConfigValue());
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return find(key).filter(value -> !value.isBlank()).orElse(defaultValue);
    }

    @Override
    public List<ConfigVO> list() {
        return sysConfigMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                        .orderByAsc(SysConfig::getId))
                .stream()
                .map(ConfigVO::from)
                .toList();
    }

    @Override
    public void update(ConfigUpdateRequest request) {
        String key = request.key().trim();
        if (!ConfigKeys.EDITABLE_KEYS.contains(key)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该配置项不支持在后台修改：" + key);
        }
        SysConfig current = sysConfigMapper.selectOne(
                Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, key).last("LIMIT 1"));
        if (current == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置项不存在：" + key);
        }
        if (ConfigKeys.RECRUIT_OPEN.equals(key) && !"0".equals(request.value()) && !"1".equals(request.value())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "报名开关只能是 0 或 1");
        }

        SysConfig update = new SysConfig();
        update.setId(current.getId());
        update.setConfigValue(request.value().trim());
        sysConfigMapper.updateById(update);

        log.info("更新系统配置：key={}", key);
    }

    @Override
    public RecruitInfoVO recruitInfo() {
        boolean open = !"0".equals(getOrDefault(ConfigKeys.RECRUIT_OPEN, DEFAULT_RECRUIT_OPEN).trim());
        return new RecruitInfoVO(
                open,
                getOrDefault(ConfigKeys.CLUB_INTRO, DEFAULT_CLUB_INTRO),
                getOrDefault(ConfigKeys.REVIEW_NOTICE, DEFAULT_REVIEW_NOTICE));
    }
}
