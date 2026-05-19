package com.suios.admin.uniform.controller;

import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.common.util.ExcelUtils;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.uniform.entity.SUniform;
import com.suios.admin.uniform.service.SUniformService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
@RequestMapping("/uniform")
@RequiredArgsConstructor
public class SUniformController {

    private final SUniformService uniformService;

    @GetMapping("/list")
    public R<PageResult<SUniform>> list(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) Long schoolId,
                                        @RequestParam(required = false) Long gradeId,
                                        @RequestParam(required = false) Long status) {
        return R.success(uniformService.list(pageNum, pageSize, name, schoolId, gradeId, status));
    }

    @GetMapping("/{id}")
    public R<SUniform> getInfo(@PathVariable Long id) {
        return R.success(uniformService.getById(id));
    }

    @PostMapping
    @OperLog(module = "校服管理", operation = "INSERT")
    public R<Void> add(@Valid @RequestBody SUniform uniform) {
        uniformService.create(uniform);
        return R.success("校服创建成功");
    }

    @PutMapping
    @OperLog(module = "校服管理", operation = "UPDATE")
    public R<Void> edit(@Valid @RequestBody SUniform uniform) {
        uniformService.update(uniform);
        return R.success("校服更新成功");
    }

    @DeleteMapping("/{ids}")
    @OperLog(module = "校服管理", operation = "DELETE")
    public R<Void> remove(@PathVariable String ids) {
        uniformService.deleteByIds(ids);
        return R.success("校服删除成功");
    }

    @PostMapping("/export")
    @OperLog(module = "校服管理", operation = "EXPORT")
    public void export(@RequestParam(required = false) String name,
                       @RequestParam(required = false) Long schoolId,
                       @RequestParam(required = false) Long gradeId,
                       @RequestParam(required = false) Long status,
                       HttpServletResponse response) throws IOException {
        List<SUniform> rows = uniformService.listAll(name, schoolId, gradeId, status);
        List<List<?>> dataRows = new ArrayList<>();
        for (SUniform item : rows) {
            dataRows.add(List.of(
                    item.getName(),
                    item.getSchoolName(),
                    item.getGradeName(),
                    item.getPrice(),
                    item.getStatus(),
                    item.getAverageRating(),
                    item.getReviewCount()
            ));
        }
        ExcelUtils.export(
                "uniforms",
                List.of("名称", "学校", "年级", "价格", "状态", "平均评分", "评论数"),
                dataRows,
                response
        );
    }
}
