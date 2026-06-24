package com.nbbackup.controller;

import com.nbbackup.common.R;
import com.nbbackup.dao.DatabaseHelper;
import com.nbbackup.model.Brand;
import com.nbbackup.model.CommandTemplate;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final DatabaseHelper db = DatabaseHelper.getInstance();

    @GetMapping
    public R<List<Brand>> getAllBrands() {
        try {
            List<Brand> brands = db.getAllBrands();
            return R.ok(brands);
        } catch (Exception e) {
            return R.fail("获取品牌列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{brandId}/commands")
    public R<List<CommandTemplate>> getCommandsByBrand(@PathVariable int brandId) {
        try {
            List<CommandTemplate> commands = db.getCommandsByBrandId(brandId);
            return R.ok(commands);
        } catch (Exception e) {
            return R.fail("获取命令模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/commands/{commandId}")
    public R<String> updateCommand(@PathVariable int commandId,
                                   @RequestBody CommandTemplate template) {
        try {
            boolean success = db.updateCommand(commandId, template.getCommand());
            if (success) {
                return R.ok("命令更新成功", null);
            }
            return R.fail("命令更新失败");
        } catch (Exception e) {
            return R.fail("更新命令失败: " + e.getMessage());
        }
    }

    @PostMapping("/commands/batch-update")
    public R<String> batchUpdateCommands(@RequestBody List<CommandTemplate> commands) {
        try {
            int count = 0;
            for (CommandTemplate cmd : commands) {
                if (db.updateCommand(cmd.getId(), cmd.getCommand())) {
                    count++;
                }
            }
            return R.ok("成功更新 " + count + " 条命令", null);
        } catch (Exception e) {
            return R.fail("批量更新失败: " + e.getMessage());
        }
    }
}
