package com.opsflow.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.auth.mapper.SysDeptMapper;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.dto.DeptDTO;
import com.opsflow.module.auth.model.entity.SysDept;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.auth.model.vo.DeptTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门管理服务：树形结构、新增、修改、删除、启停用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    /**
     * 查询部门树（可按名称/状态过滤）
     */
    public List<DeptTreeVO> tree(String deptName, Integer status) {
        List<SysDept> depts = deptMapper.selectList(
                Wrappers.<SysDept>lambdaQuery()
                        .like(StringUtils.hasText(deptName), SysDept::getDeptName, deptName)
                        .eq(status != null, SysDept::getStatus, status)
                        .orderByAsc(SysDept::getSortOrder)
                        .orderByAsc(SysDept::getId));
        return buildTree(depts);
    }

    /**
     * 新增部门
     */
    public void create(DeptDTO dto, String operator) {
        validateParent(dto.getParentId(), null);
        SysDept dept = new SysDept();
        copyToEntity(dto, dept);
        dept.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        dept.setCreateBy(operator);
        deptMapper.insert(dept);
        log.info("新增部门: deptName={}, operator={}", dto.getDeptName(), operator);
    }

    /**
     * 修改部门
     */
    public void update(Long id, DeptDTO dto, String operator) {
        SysDept dept = getById(id);
        validateParent(dto.getParentId(), id);
        copyToEntity(dto, dept);
        dept.setUpdateBy(operator);
        deptMapper.updateById(dept);
    }

    /**
     * 删除部门（存在子部门或部门下存在用户时不可删除）
     */
    public void delete(Long id) {
        getById(id);
        Long childCount = deptMapper.selectCount(
                Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
        }
        Long userCount = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_USERS);
        }
        deptMapper.deleteById(id);
        log.info("删除部门: id={}", id);
    }

    /**
     * 启停用部门
     */
    public void changeStatus(Long id, Integer status) {
        SysDept dept = getById(id);
        dept.setStatus(status);
        deptMapper.updateById(dept);
    }

    /**
     * 校验父部门合法性：父部门不能是自身或其子部门，且必须存在
     */
    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (currentId != null && parentId.equals(currentId)) {
            throw new BusinessException(ErrorCode.DEPT_PARENT_INVALID);
        }
        if (deptMapper.selectById(parentId) == null) {
            throw new BusinessException(ErrorCode.DEPT_PARENT_INVALID);
        }
        if (currentId != null) {
            List<SysDept> all = deptMapper.selectList(null);
            Map<Long, Long> parentMap = new HashMap<>();
            for (SysDept d : all) {
                parentMap.put(d.getId(), d.getParentId());
            }
            Long cursor = parentId;
            while (cursor != null && cursor != 0) {
                if (cursor.equals(currentId)) {
                    throw new BusinessException(ErrorCode.DEPT_PARENT_INVALID);
                }
                cursor = parentMap.get(cursor);
            }
        }
    }

    private SysDept getById(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
        return dept;
    }

    private void copyToEntity(DeptDTO dto, SysDept dept) {
        dept.setDeptName(dto.getDeptName());
        dept.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        dept.setRemark(dto.getRemark());
        if (dto.getParentId() != null) {
            dept.setParentId(dto.getParentId());
        }
    }

    private List<DeptTreeVO> buildTree(List<SysDept> depts) {
        Map<Long, DeptTreeVO> nodeMap = new HashMap<>();
        for (SysDept d : depts) {
            DeptTreeVO vo = new DeptTreeVO();
            BeanUtils.copyProperties(d, vo);
            vo.setChildren(new ArrayList<>());
            nodeMap.put(d.getId(), vo);
        }
        List<DeptTreeVO> roots = new ArrayList<>();
        for (DeptTreeVO vo : nodeMap.values()) {
            DeptTreeVO parent = nodeMap.get(vo.getParentId());
            if (parent != null && !vo.getId().equals(vo.getParentId())) {
                parent.getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }
}