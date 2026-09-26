package com.tsguosc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tsguosc.common.constant.DictType;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.dto.FeedbackHandleRequest;
import com.tsguosc.dto.FeedbackQuery;
import com.tsguosc.dto.FeedbackSubmitRequest;
import com.tsguosc.dto.FeedbackVO;
import com.tsguosc.dto.PageResult;
import com.tsguosc.entity.Feedback;
import com.tsguosc.entity.SysDict;
import com.tsguosc.mapper.FeedbackMapper;
import com.tsguosc.mapper.SysDictMapper;
import com.tsguosc.service.FeedbackService;
import com.tsguosc.util.CaptchaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 轻量反馈实现（F-014）。
 *
 * <p>两个要点：
 *   <ul>
 *     <li>提交是**免登录**接口，所以第一件事必须是消费一次性图形验证码（与报名 / 查询同源）——
 *         没有它就等于对外开放了一个无限灌垃圾的写接口。</li>
 *     <li>内容按**纯文本**存取：前端用 `{{ }}` 渲染（Vue 默认转义），天然没有 XSS 问题，
 *         因此这里不做富文本清洗（与公告 F-008 不同，那条路才有 HTML）。</li>
 *   </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    /** 单页上限，避免前端传个巨大的 size 把库拖死 */
    private static final int MAX_PAGE_SIZE = 100;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final FeedbackMapper feedbackMapper;

    private final SysDictMapper sysDictMapper;

    private final CaptchaValidator captchaValidator;

    @Override
    public void submit(FeedbackSubmitRequest request) {
        // ① 一次性验证码（免登录接口的门槛）
        captchaValidator.validateAndConsume(request.captchaKey(), request.captchaCode());

        // ② 来源必须是字典里的启用项（防止乱塞数字）
        requireEnabledDict(DictType.FEEDBACK_SOURCE, String.valueOf(request.source()));

        Feedback entity = new Feedback();
        entity.setContent(request.content().trim());
        entity.setContact(StringUtils.hasText(request.contact()) ? request.contact().trim() : null);
        entity.setSource(request.source());
        entity.setHandled(0);
        feedbackMapper.insert(entity);
        log.info("收到反馈：id={}, source={}", entity.getId(), entity.getSource());
    }

    @Override
    public PageResult<FeedbackVO> list(FeedbackQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(query.getSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<Feedback> wrapper = Wrappers.<Feedback>lambdaQuery()
                .eq(query.getSource() != null, Feedback::getSource, query.getSource())
                .eq(query.getHandled() != null, Feedback::getHandled, query.getHandled())
                .orderByDesc(Feedback::getCreatedAt)
                .orderByDesc(Feedback::getId);

        Page<Feedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);
        List<FeedbackVO> records = result.getRecords().stream().map(FeedbackVO::from).toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    @Override
    public void handle(FeedbackHandleRequest request) {
        if (feedbackMapper.selectById(request.id()) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "反馈不存在或已被删除");
        }
        // handled 是 0/1，用显式 set（与项目里"可空字段一律走 LambdaUpdateWrapper"的口径一致）
        feedbackMapper.update(null, Wrappers.<Feedback>lambdaUpdate()
                .eq(Feedback::getId, request.id())
                .set(Feedback::getHandled, request.handled()));
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    /** 字典编码必须存在且处于启用状态 */
    private void requireEnabledDict(DictType type, String code) {
        boolean exists = sysDictMapper.exists(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getType, type.getCode())
                .eq(SysDict::getCode, code.trim())
                .eq(SysDict::getEnabled, 1));
        if (!exists) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    type.getLabel() + "选项不合法，请刷新页面后重试");
        }
    }
}
