package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.AnnouncementDetailVO;
import com.tsguosc.dto.AnnouncementQuery;
import com.tsguosc.dto.AnnouncementSaveRequest;
import com.tsguosc.dto.AnnouncementVO;
import com.tsguosc.dto.PageResult;
import com.tsguosc.entity.Announcement;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.AnnouncementMapper;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.AnnouncementService;
import com.tsguosc.util.AnnouncementImageStorage;
import com.tsguosc.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公告系统实现（PRD F-008）。
 *
 * <p>三件事在这里收口：
 * <ol>
 *   <li><b>排序</b>：恒为 {@code is_top DESC, created_at DESC}（PRD「置顶公告始终排在列表最前」），
 *       再按 id 兜底 —— 同一秒发布的两条公告也要有稳定顺序，否则分页会重复/漏项</li>
 *   <li><b>XSS 清洗</b>：写入前 {@code cleanForStore}（图片地址压成对象 key）、
 *       读取后 {@code cleanForOutput}（key 拼回地址）</li>
 *   <li><b>作者名组装</b>：表里只存 {@code created_by}，姓名按 id 批量查一次再拼，
 *       不在 SQL 里 join（沿用「不建物理外键、应用层组装」的既有口径 §6 D18）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final HtmlSanitizer htmlSanitizer;
    private final AnnouncementImageStorage imageStorage;

    // ------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------

    @Override
    public PageResult<AnnouncementVO> list(AnnouncementQuery query) {
        LambdaQueryWrapper<Announcement> wrapper = Wrappers.lambdaQuery();
        String keyword = trimToNull(query == null ? null : query.getKeyword());
        if (keyword != null) {
            wrapper.like(Announcement::getTitle, keyword);
        }
        // 置顶优先 → 发布时间倒序 → id 倒序（同一秒发布时保证顺序稳定）
        wrapper.orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreatedAt)
                .orderByDesc(Announcement::getId);

        Page<Announcement> page = new Page<>(
                normalizePage(query == null ? null : query.getPage()),
                normalizeSize(query == null ? null : query.getSize()));
        IPage<Announcement> result = announcementMapper.selectPage(page, wrapper);
        Map<Long, String> authorNames = authorNames(result.getRecords());

        List<AnnouncementVO> records = result.getRecords().stream()
                .map(item -> new AnnouncementVO(
                        item.getId(),
                        item.getTitle(),
                        htmlSanitizer.toSummary(item.getContent()),
                        item.getIsTop(),
                        authorNames.get(item.getCreatedBy()),
                        item.getCreatedAt(),
                        item.getUpdatedAt()))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public AnnouncementDetailVO detail(Long id) {
        Announcement announcement = requireAnnouncement(id);
        Map<Long, String> authorNames = authorNames(List.of(announcement));
        return new AnnouncementDetailVO(
                announcement.getId(),
                announcement.getTitle(),
                // 输出端再清一次：防直连库写入的脏数据、以及早期未清洗的历史数据进浏览器
                htmlSanitizer.cleanForOutput(announcement.getContent()),
                announcement.getIsTop(),
                authorNames.get(announcement.getCreatedBy()),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt());
    }

    // ------------------------------------------------------------
    // 写入
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AnnouncementSaveRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title().trim());
        announcement.setContent(requireCleanContent(request.content()));
        announcement.setIsTop(isTop(request.isTop()));
        announcementMapper.insert(announcement);
        log.info("公告已发布：id={}, title={}, isTop={}",
                announcement.getId(), announcement.getTitle(), announcement.getIsTop());
        return announcement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AnnouncementSaveRequest request) {
        requireAnnouncement(id);
        String content = requireCleanContent(request.content());

        // ⚠️ 显式 set：取消置顶（1 → 0）必须真落库，
        // 走 updateById 会在字段为 null 时被忽略（§6 D71 的同类坑）
        announcementMapper.update(null, Wrappers.<Announcement>lambdaUpdate()
                .eq(Announcement::getId, id)
                .set(Announcement::getTitle, request.title().trim())
                .set(Announcement::getContent, content)
                .set(Announcement::getIsTop, isTop(request.isTop())));
        log.info("公告已更新：id={}, isTop={}", id, isTop(request.isTop()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireAnnouncement(id);
        // 逻辑删除（@TableLogic）：库里留痕，误删可从库中恢复
        announcementMapper.deleteById(id);
        log.info("公告已删除（逻辑）：id={}", id);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return imageStorage.publicUrl(imageStorage.upload(file));
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    /**
     * 清洗正文并确认清洗后还有内容。
     *
     * <p>只提交 {@code <script>} / {@code <iframe>} 这类全被剥掉的内容时，
     * 入库会变成空串 —— 与其存一条「打不开的公告」，不如当场告诉作者内容不受支持。
     */
    private String requireCleanContent(String rawContent) {
        String cleaned = htmlSanitizer.cleanForStore(rawContent);
        if (!htmlSanitizer.hasVisibleContent(cleaned)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "公告内容为空或仅包含不受支持的格式，请检查后重新提交");
        }
        return cleaned;
    }

    private Announcement requireAnnouncement(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少公告 id");
        }
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在或已被删除");
        }
        return announcement;
    }

    /** 批量取作者姓名：一次查询覆盖整页，避免逐行查库 */
    private Map<Long, String> authorNames(List<Announcement> announcements) {
        Set<Long> ids = announcements.stream()
                .map(Announcement::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
    }

    private Integer isTop(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
