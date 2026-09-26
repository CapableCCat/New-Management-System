package com.tsguosc.util;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.entity.SysDict;
import com.tsguosc.mapper.SysDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典索引（跨任务点共用的一份实现）。
 *
 * <p>项目里反复需要「把字典整个 load 进内存再查」：T13 导入要 label→code（人填中文名，落库要 code）、
 * T14 导出与 T15 看板要 code→label（库里存 code，给人看要中文名）。各写一份必然漂 ——
 * 抽到这里，两个方向都提供，**是否只认启用项由调用方决定**（因为这正是两个方向的差异所在）：
 *
 * <ul>
 *   <li>{@link #labelToCode()} —— **只含启用项**：导入场景，停用项不该再被填进来</li>
 *   <li>{@link #codeToLabel()} —— **含停用项**：展示场景，历史数据引用了后来被停用的条目时，
 *       仍要能显示它的名字</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class DictIndex {

    private final SysDictMapper sysDictMapper;

    /** type → (label → code)，仅启用项 */
    public Map<String, Map<String, String>> labelToCode() {
        Map<String, Map<String, String>> index = new HashMap<>();
        for (SysDict dict : load(1)) {
            if (dict.getLabel() == null || dict.getCode() == null) {
                continue;
            }
            index.computeIfAbsent(dict.getType(), key -> new HashMap<>())
                    .put(dict.getLabel().trim(), dict.getCode().trim());
        }
        return index;
    }

    /** type → (code → label)，**含停用项** */
    public Map<String, Map<String, String>> codeToLabel() {
        Map<String, Map<String, String>> index = new HashMap<>();
        for (SysDict dict : load(null)) {
            if (dict.getLabel() == null || dict.getCode() == null) {
                continue;
            }
            index.computeIfAbsent(dict.getType(), key -> new HashMap<>())
                    .put(dict.getCode().trim(), dict.getLabel().trim());
        }
        return index;
    }

    private List<SysDict> load(Integer enabled) {
        return enabled == null
                ? sysDictMapper.selectList(Wrappers.<SysDict>lambdaQuery())
                : sysDictMapper.selectList(Wrappers.<SysDict>lambdaQuery().eq(SysDict::getEnabled, enabled));
    }
}
