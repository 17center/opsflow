package com.opsflow.module.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.system.mapper.SysNotificationMapper;
import com.opsflow.module.system.model.entity.SysNotification;
import com.opsflow.module.system.model.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内通知服务：创建通知、分页查询、未读数、已读标记
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SysNotificationMapper notificationMapper;

    /**
     * 创建站内通知
     *
     * @param userId      接收人 ID
     * @param title       通知标题
     * @param content     通知内容
     * @param notifyType  通知类型：1=工单 2=审批 3=告警 4=系统
     * @param relatedId   关联业务 ID
     * @param relatedType 关联业务类型（TICKET/ALERT）
     */
    public void notify(Long userId, String title, String content, Integer notifyType,
                       Long relatedId, String relatedType) {
        if (userId == null) {
            return;
        }
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotifyType(notifyType);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    /**
     * 分页查询当前用户通知
     */
    public PageResult<NotificationVO> page(Long userId, long current, long size, Integer isRead) {
        Page<SysNotification> page = notificationMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysNotification>lambdaQuery()
                        .eq(SysNotification::getUserId, userId)
                        .eq(isRead != null, SysNotification::getIsRead, isRead)
                        .orderByDesc(SysNotification::getId));
        List<NotificationVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 未读通知数
     */
    public Long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                Wrappers.<SysNotification>lambdaQuery()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0));
    }

    /**
     * 标记单条已读
     */
    public void markRead(Long id, Long userId) {
        SysNotification notification = notificationMapper.selectOne(
                Wrappers.<SysNotification>lambdaQuery()
                        .eq(SysNotification::getId, id)
                        .eq(SysNotification::getUserId, userId));
        if (notification == null) {
            return;
        }
        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);
    }

    /**
     * 全部标记已读
     */
    public void markAllRead(Long userId) {
        List<SysNotification> unread = notificationMapper.selectList(
                Wrappers.<SysNotification>lambdaQuery()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getIsRead, 0));
        for (SysNotification notification : unread) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
    }

    private NotificationVO toVO(SysNotification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setNotifyType(notification.getNotifyType());
        vo.setRelatedId(notification.getRelatedId());
        vo.setRelatedType(notification.getRelatedType());
        vo.setIsRead(notification.getIsRead());
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }
}