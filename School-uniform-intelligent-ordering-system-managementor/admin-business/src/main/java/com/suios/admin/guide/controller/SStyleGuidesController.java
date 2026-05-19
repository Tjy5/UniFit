package com.suios.admin.guide.controller;

import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.guide.entity.SStyleGuides;
import com.suios.admin.guide.service.SStyleGuidesService;
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
@RequestMapping("/guides")
@RequiredArgsConstructor
public class SStyleGuidesController {

    private final SStyleGuidesService guidesService;

    @GetMapping("/list")
    public R<PageResult<SStyleGuides>> list(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String title,
                                            @RequestParam(required = false) Long uniformId,
                                            @RequestParam(required = false) Long status) {
        return R.success(guidesService.list(pageNum, pageSize, title, uniformId, status));
    }

    @GetMapping("/{id}")
    public R<SStyleGuides> getInfo(@PathVariable Long id) {
        return R.success(guidesService.getById(id));
    }

    @PostMapping
    @OperLog(module = "穿搭指南", operation = "INSERT")
    public R<Void> add(@Valid @RequestBody SStyleGuides guide) {
        guidesService.create(guide);
        return R.success("穿搭指南创建成功");
    }

    @PutMapping
    @OperLog(module = "穿搭指南", operation = "UPDATE")
    public R<Void> edit(@Valid @RequestBody SStyleGuides guide) {
        guidesService.update(guide);
        return R.success("穿搭指南更新成功");
    }

    @DeleteMapping("/{ids}")
    @OperLog(module = "穿搭指南", operation = "DELETE")
    public R<Void> remove(@PathVariable String ids) {
        guidesService.deleteByIds(ids);
        return R.success("穿搭指南删除成功");
    }
}
