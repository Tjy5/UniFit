package com.suios.admin.size.controller;

import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.common.util.ExcelUtils;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.size.entity.SSizes;
import com.suios.admin.size.service.SSizesService;
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
@RequestMapping("/sizes")
@RequiredArgsConstructor
public class SSizesController {

    private final SSizesService sizesService;

    @GetMapping("/list")
    public R<PageResult<SSizes>> list(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String sizeName) {
        return R.success(sizesService.list(pageNum, pageSize, sizeName));
    }

    @GetMapping("/{id}")
    public R<SSizes> getInfo(@PathVariable Long id) {
        return R.success(sizesService.getById(id));
    }

    @PostMapping
    @OperLog(module = "尺码管理", operation = "INSERT")
    public R<Void> add(@Valid @RequestBody SSizes sizes) {
        sizesService.create(sizes);
        return R.success("尺码创建成功");
    }

    @PutMapping
    @OperLog(module = "尺码管理", operation = "UPDATE")
    public R<Void> edit(@Valid @RequestBody SSizes sizes) {
        sizesService.update(sizes);
        return R.success("尺码更新成功");
    }

    @DeleteMapping("/{ids}")
    @OperLog(module = "尺码管理", operation = "DELETE")
    public R<Void> remove(@PathVariable String ids) {
        sizesService.deleteByIds(ids);
        return R.success("尺码删除成功");
    }

    @PostMapping("/export")
    @OperLog(module = "尺码管理", operation = "EXPORT")
    public void export(@RequestParam(required = false) String sizeName, HttpServletResponse response) throws IOException {
        List<SSizes> rows = sizesService.listAll(sizeName);
        List<List<?>> dataRows = new ArrayList<>();
        for (SSizes item : rows) {
            dataRows.add(List.of(
                    item.getSizeName(),
                    item.getMinHeight(),
                    item.getMaxHeight(),
                    item.getMinWeight(),
                    item.getMaxWeight(),
                    item.getMinChest(),
                    item.getMaxChest(),
                    item.getMinWaist(),
                    item.getMaxWaist(),
                    item.getMinHip(),
                    item.getMaxHip(),
                    item.getMinShoulder(),
                    item.getMaxShoulder()
            ));
        }
        ExcelUtils.export(
                "sizes",
                List.of("名称", "最低身高", "最高身高", "最小体重", "最大体重", "最小胸围", "最大胸围", "最小腰围", "最大腰围", "最小臀围", "最大臀围", "最小肩宽", "最大肩宽"),
                dataRows,
                response
        );
    }
}
