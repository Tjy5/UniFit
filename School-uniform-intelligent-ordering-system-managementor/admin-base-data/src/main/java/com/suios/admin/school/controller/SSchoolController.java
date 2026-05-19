package com.suios.admin.school.controller;

import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.school.entity.SSchool;
import com.suios.admin.school.service.SSchoolService;
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
@RequestMapping("/schools")
@RequiredArgsConstructor
public class SSchoolController {

    private final SSchoolService schoolService;

    @GetMapping("/list")
    public R<PageResult<SSchool>> list(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       @RequestParam(required = false) String schoolName) {
        return R.success(schoolService.list(pageNum, pageSize, schoolName));
    }

    @GetMapping("/selectList")
    public R<?> selectList() {
        return R.success(schoolService.selectList());
    }

    @GetMapping("/{schoolId}")
    public R<SSchool> getInfo(@PathVariable Long schoolId) {
        return R.success(schoolService.getById(schoolId));
    }

    @PostMapping
    @OperLog(module = "学校管理", operation = "INSERT")
    public R<Void> add(@Valid @RequestBody SSchool school) {
        schoolService.create(school);
        return R.success("学校创建成功");
    }

    @PutMapping
    @OperLog(module = "学校管理", operation = "UPDATE")
    public R<Void> edit(@Valid @RequestBody SSchool school) {
        schoolService.update(school);
        return R.success("学校更新成功");
    }

    @DeleteMapping("/{ids}")
    @OperLog(module = "学校管理", operation = "DELETE")
    public R<Void> remove(@PathVariable String ids) {
        schoolService.deleteByIds(ids);
        return R.success("学校删除成功");
    }
}
