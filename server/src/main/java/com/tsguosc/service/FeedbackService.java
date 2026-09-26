package com.tsguosc.service;

import com.tsguosc.dto.FeedbackHandleRequest;
import com.tsguosc.dto.FeedbackQuery;
import com.tsguosc.dto.FeedbackSubmitRequest;
import com.tsguosc.dto.FeedbackVO;
import com.tsguosc.dto.PageResult;

/**
 * 轻量反馈（F-014）：提交免登录，查看限社长团 / 超管。
 */
public interface FeedbackService {

    /** 提交反馈（带一次性图形验证码） */
    void submit(FeedbackSubmitRequest request);

    /** 管理端：反馈列表（来源 / 处理状态可筛，新→旧） */
    PageResult<FeedbackVO> list(FeedbackQuery query);

    /** 管理端：标记已处理 / 未处理 */
    void handle(FeedbackHandleRequest request);
}
