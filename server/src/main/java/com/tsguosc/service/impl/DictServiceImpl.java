package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.DictCreateRequest;
import com.tsguosc.dto.DictTypeVO;
import com.tsguosc.dto.DictUpdateRequest;
import com.tsguosc.dto.DictVO;
import com.tsguosc.entity.SysDict;
import com.tsguosc.mapper.SysDictMapper;
import com.tsguosc.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 字典管理实现。
 *
 * <p>设计要点：
 * <ul>
 *   <li>不做缓存：数据量极小，且 PRD F-011 要求"改文案后各页同步生效"，直接查库最稳</li>
 *   <li>删除能力不提供，只用 enabled 停用（D19）；但**不允许把某类型的启用项停空**</li>
 *   <li>排序按 sort 升序；上移/下移先归一化为 10/20/30… 再交换，避免 sort 相同导致顺序不稳</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    /** 排序归一化步长 */
    private static final int SORT_STEP = 10;

    private final SysDictMapper sysDictMapper;

    @Override
    public List<DictTypeVO> types() {
        return Arrays.stream(DictType.values())
                .map(t -> new DictTypeVO(t.getCode(), t.getLabel(), t.isCore()))
                .toList();
    }

    @Override
    public List<DictVO> listEnabled(String type) {
        DictType dictType = DictType.of(type);
        return listEntities(dictType.getCode(), true).stream().map(DictVO::from).toList();
    }

    @Override
    public List<DictVO> listAll(String type) {
        DictType dictType = DictType.of(type);
        return listEntities(dictType.getCode(), false).stream().map(DictVO::from).toList();
    }

    @Override
    public void create(DictCreateRequest request) {
        DictType type = DictType.of(request.type());
        String code = request.code().trim();
        String label = request.label().trim();

        boolean exists = sysDictMapper.exists(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getType, type.getCode())
                .eq(SysDict::getCode, code));
        if (exists) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "「" + type.getLabel() + "」下已存在编码 " + code);
        }

        SysDict entity = new SysDict();
        entity.setType(type.getCode());
        entity.setCode(code);
        entity.setLabel(label);
        entity.setSort(request.sort() != null ? request.sort() : nextSort(type.getCode()));
        entity.setEnabled(1);
        entity.setRemark(request.remark());
        sysDictMapper.insert(entity);

        log.info("新增字典：type={}, code={}, label={}", type.getCode(), code, label);
    }

    @Override
    public void update(Long id, DictUpdateRequest request) {
        SysDict current = require(id);
        DictType type = DictType.of(current.getType());

        SysDict update = new SysDict();
        update.setId(id);
        update.setLabel(request.label().trim());
        if (request.sort() != null) {
            update.setSort(request.sort());
        }
        if (request.remark() != null) {
            update.setRemark(request.remark());
        }
        if (request.enabled() != null) {
            if (request.enabled() == 0) {
                assertNotLastEnabled(type, id);
            }
            update.setEnabled(request.enabled());
        }
        sysDictMapper.updateById(update);

        log.info("更新字典：id={}, type={}, code={}, label={}, enabled={}",
                id, type.getCode(), current.getCode(), request.label(), request.enabled());
    }

    @Override
    public void move(Long id, String direction) {
        if (!"up".equals(direction) && !"down".equals(direction)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "direction 只能是 up 或 down");
        }
        SysDict current = require(id);

        // 先归一化排序，保证顺序确定（避免出现 sort 相同的并列项）
        reorder(current.getType());

        List<SysDict> list = listEntities(current.getType(), false);
        int index = indexOf(list, id);
        int targetIndex = "up".equals(direction) ? index - 1 : index + 1;
        if (index < 0 || targetIndex < 0 || targetIndex >= list.size()) {
            // 已经在顶部 / 底部，静默返回（前端按钮也会置灰）
            return;
        }
        SysDict target = list.get(targetIndex);
        updateSort(current.getId(), target.getSort());
        updateSort(target.getId(), current.getSort());
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    private List<SysDict> listEntities(String typeCode, boolean onlyEnabled) {
        return sysDictMapper.selectList(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getType, typeCode)
                .eq(onlyEnabled, SysDict::getEnabled, 1)
                .orderByAsc(SysDict::getSort)
                .orderByAsc(SysDict::getId));
    }

    private SysDict require(Long id) {
        SysDict entity = sysDictMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "字典条目不存在");
        }
        return entity;
    }

    /** 新条目排到该类型末尾 */
    private int nextSort(String typeCode) {
        List<SysDict> list = listEntities(typeCode, false);
        int max = 0;
        for (SysDict entity : list) {
            if (entity.getSort() != null && entity.getSort() > max) {
                max = entity.getSort();
            }
        }
        return max + SORT_STEP;
    }

    /** 把某类型的排序重排为 10/20/30… */
    private void reorder(String typeCode) {
        List<SysDict> list = listEntities(typeCode, false);
        int seq = SORT_STEP;
        for (SysDict entity : list) {
            if (!Objects.equals(entity.getSort(), seq)) {
                updateSort(entity.getId(), seq);
            }
            seq += SORT_STEP;
        }
    }

    private void updateSort(Long id, Integer sort) {
        if (sort == null) {
            return;
        }
        SysDict update = new SysDict();
        update.setId(id);
        update.setSort(sort);
        sysDictMapper.updateById(update);
    }

    /** 不允许把某类型的启用项停空（否则报名页对应下拉会变空） */
    private void assertNotLastEnabled(DictType type, Long excludeId) {
        Long enabledCount = sysDictMapper.selectCount(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getType, type.getCode())
                .eq(SysDict::getEnabled, 1)
                .ne(SysDict::getId, excludeId));
        if (enabledCount == null || enabledCount == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "「" + type.getLabel() + "」至少要保留一个启用项，不能全部停用");
        }
    }

    private int indexOf(List<SysDict> list, Long id) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getId(), id)) {
                return i;
            }
        }
        return -1;
    }
}
