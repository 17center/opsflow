package com.opsflow.module.alert.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.alert.mapper.AlertDutyScheduleMapper;
import com.opsflow.module.alert.model.dto.AlertDutyDTO;
import com.opsflow.module.alert.model.entity.AlertDutySchedule;
import com.opsflow.module.alert.model.vo.AlertDutyVO;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 值班排班服务：按月查询、创建、修改、删除
 */
@Service
@RequiredArgsConstructor
public class AlertDutyService {

    private static final String[] SHIFT_NAMES = {"", "全天", "白班", "夜班"};

    private final AlertDutyScheduleMapper dutyMapper;
    private final SysUserMapper userMapper;

    /** 按月查询排班 */
    public List<AlertDutyVO> listByMonth(String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<AlertDutySchedule> list = dutyMapper.selectList(
                Wrappers.<AlertDutySchedule>lambdaQuery()
                        .ge(AlertDutySchedule::getDutyDate, start)
                        .le(AlertDutySchedule::getDutyDate, end)
                        .orderByAsc(AlertDutySchedule::getDutyDate));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public void create(AlertDutyDTO dto, String operator) {
        checkUnique(dto.getUserId(), dto.getDutyDate(), dto.getShiftType(), null);
        if (userMapper.selectById(dto.getUserId()) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        AlertDutySchedule duty = new AlertDutySchedule();
        duty.setUserId(dto.getUserId());
        duty.setDutyDate(dto.getDutyDate());
        duty.setShiftType(dto.getShiftType());
        duty.setCreateBy(operator);
        dutyMapper.insert(duty);
    }

    @Transactional
    public void update(Long id, AlertDutyDTO dto, String operator) {
        AlertDutySchedule duty = requireDuty(id);
        checkUnique(dto.getUserId(), dto.getDutyDate(), dto.getShiftType(), id);
        if (userMapper.selectById(dto.getUserId()) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        duty.setUserId(dto.getUserId());
        duty.setDutyDate(dto.getDutyDate());
        duty.setShiftType(dto.getShiftType());
        duty.setUpdateBy(operator);
        dutyMapper.updateById(duty);
    }

    @Transactional
    public void delete(Long id) {
        requireDuty(id);
        dutyMapper.deleteById(id);
    }

    /** 某日值班人（全天=1，白班=2，夜班=3；优先全天，其次白班） */
    public List<Long> getDutyUserIds(LocalDate date) {
        return dutyMapper.selectList(
                        Wrappers.<AlertDutySchedule>lambdaQuery()
                                .eq(AlertDutySchedule::getDutyDate, date)
                                .orderByAsc(AlertDutySchedule::getShiftType))
                .stream().map(AlertDutySchedule::getUserId).distinct().collect(Collectors.toList());
    }

    private void checkUnique(Long userId, LocalDate date, Integer shiftType, Long excludeId) {
        Long count = dutyMapper.selectCount(
                Wrappers.<AlertDutySchedule>lambdaQuery()
                        .eq(AlertDutySchedule::getUserId, userId)
                        .eq(AlertDutySchedule::getDutyDate, date)
                        .eq(AlertDutySchedule::getShiftType, shiftType)
                        .ne(excludeId != null, AlertDutySchedule::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.ALERT_DUTY_EXISTS);
        }
    }

    private AlertDutySchedule requireDuty(Long id) {
        AlertDutySchedule duty = dutyMapper.selectById(id);
        if (duty == null) {
            throw new BusinessException(ErrorCode.ALERT_DUTY_NOT_FOUND);
        }
        return duty;
    }

    private AlertDutyVO toVO(AlertDutySchedule duty) {
        AlertDutyVO vo = new AlertDutyVO();
        vo.setId(duty.getId());
        vo.setUserId(duty.getUserId());
        SysUser user = userMapper.selectById(duty.getUserId());
        if (user != null) {
            vo.setUserName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        }
        vo.setDutyDate(duty.getDutyDate());
        vo.setShiftType(duty.getShiftType());
        vo.setShiftTypeName(duty.getShiftType() != null && duty.getShiftType() < SHIFT_NAMES.length
                ? SHIFT_NAMES[duty.getShiftType()] : String.valueOf(duty.getShiftType()));
        return vo;
    }
}