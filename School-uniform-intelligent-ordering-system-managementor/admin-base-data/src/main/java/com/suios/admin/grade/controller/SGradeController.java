package com.suios.admin.grade.controller;

import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.grade.entity.SGrade;
import com.suios.admin.grade.service.SGradeService;
import com.suios.admin.common.annotation.OperLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grades")
@RequiredArgsConstructor
public class SGradeController {

    private final SGradeService gradeService;

    @GetMapping("/list")
    public R<PageResult<SGrade>> list(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String gradeName,
                                      @RequestParam(required = false) Long schoolId) {
        return R.success(gradeService.list(pageNum, pageSize, gradeName, schoolId));
    }

    @GetMapping("/selectList")
    public R<?> selectList(@RequestParam(required = false) Long schoolId) {
        return R.success(gradeService.selectList(schoolId));
    }

    @GetMapping("/{gradeId}")
    public R<SGrade> getInfo(@PathVariable Long gradeId) {
        return R.success(gradeService.getById(gradeId));
    }

    @PostMapping
    @OperLog(module = "年级管理", operation = "INSERT")
    public R<Void> add(@Valid @RequestBody SGrade grade) {
        gradeService.create(grade);
        return R.success("年级创建成功");
    }

    @PutMapping
    @OperLog(module = "年级管理", operation = "UPDATE")
    public R<Void> edit(@Valid @RequestBody SGrade grade) {
        gradeService.update(grade);
        return R.success("年级更新成功");
    }

    @DeleteMapping("/{ids}")
    @OperLog(module = "年级管理", operation = "DELETE")
    public R<Void> remove(@PathVariable String ids) {
        gradeService.deleteByIds(ids);
        return R.success("年级删除成功");
    }
}
