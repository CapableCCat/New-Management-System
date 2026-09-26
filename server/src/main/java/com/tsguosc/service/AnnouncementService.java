package com.tsguosc.service;

import com.tsguosc.dto.AnnouncementDetailVO;
import com.tsguosc.dto.AnnouncementQuery;
import com.tsguosc.dto.AnnouncementSaveRequest;
import com.tsguosc.dto.AnnouncementVO;
import com.tsguosc.dto.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公告系统（PRD F-008）。
 *
 * <p>权限边界：读取对所有登录用户开放；发布 / 编辑 / 删除限超管 / 社长团 / 部长
 * （PRD 第五章权限矩阵「发布公告」一行）—— 判定在 Controller 的角色注解上，
 * 本层不再重复推导角色。
 *
 * <p>XSS 清洗在本层统一收口：写入前 {@code cleanForStore}、读取后 {@code cleanForOutput}，
 * 任何调用方都拿不到未清洗的内容。
 */
public interface AnnouncementService {

    /** 公告列表（置顶优先 + 发布时间倒序；摘要由正文剥标签生成，列表不含正文） */
    PageResult<AnnouncementVO> list(AnnouncementQuery query);

    /** 公告详情（正文为已清洗的富文本 HTML） */
    AnnouncementDetailVO detail(Long id);

    /** 发布公告，返回新公告 id */
    Long create(AnnouncementSaveRequest request);

    /** 编辑公告（整体提交：标题 / 正文 / 置顶都要带上，置顶取消也走这里） */
    void update(Long id, AnnouncementSaveRequest request);

    /** 删除公告（逻辑删除，可在库里追溯） */
    void delete(Long id);

    /** 上传公告配图，返回可直接嵌入富文本的完整地址 */
    String uploadImage(MultipartFile file);
}
