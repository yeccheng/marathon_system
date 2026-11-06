package com.ape.marathon.controller;

import com.ape.marathon.common.PageResult;
import com.ape.marathon.common.Result;
import com.ape.marathon.entity.MarathonEvent;
import com.ape.marathon.service.EventService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 模块一：赛事管理
 * 基础路径：/api/event
 */
@Api(tags = "赛事管理")
@RestController
@RequestMapping("/api/event")
public class EventController {

    /** 允许的图片后缀 */
    private static final String[] ALLOWED_EXT = {"jpg", "jpeg", "png"};

    /** 单文件最大 5MB */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    @Autowired
    private EventService eventService;

    /** 本地保存目录 */
    @Value("${marathon.upload.path}")
    private String uploadPath;

    /** 对外访问前缀，如 /uploads/ */
    @Value("${marathon.upload.url-prefix:/uploads/}")
    private String urlPrefix;

    /** 1. 新增赛事：POST /api/event/add */
    @ApiOperation("新增赛事")
    @PostMapping("/add")
    public Result<java.util.Map<String, Object>> add(@Valid @RequestBody MarathonEvent event,
                                                     BindingResult bindingResult) {
        // 手动接 BindingResult：取第一个字段错误，统一 Result 返回
        if (bindingResult.hasErrors()) {
            return Result.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        return Result.success(java.util.Collections.singletonMap("id", eventService.add(event)));
    }

    /** 2. 修改赛事信息：PUT /api/event/update */
    @ApiOperation("修改赛事信息")
    @PutMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody MarathonEvent event,
                                  BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        return Result.success(eventService.update(event));
    }

    /** 3. 分页多条件查询赛事列表：GET /api/event/list */
    @ApiOperation("分页多条件查询赛事列表")
    @GetMapping("/list")
    public Result<PageResult<MarathonEvent>> list(
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "赛事名称（模糊）") @RequestParam(required = false) String eventName,
            @ApiParam(value = "赛事类型：1全程 2半程 3迷你") @RequestParam(required = false) Integer eventType,
            @ApiParam(value = "状态：0报名中 1报名截止 2已结束") @RequestParam(required = false) Integer status,
            @ApiParam(value = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @ApiParam(value = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return Result.success(eventService.page(eventName, eventType, status, startDate, endDate, pageNum, pageSize));
    }

    /** 4. 查询赛事详情（附带剩余名额）：GET /api/event/detail/{id} */
    @ApiOperation("查询赛事详情（含剩余名额）")
    @GetMapping("/detail/{id}")
    public Result<MarathonEvent> detail(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long id) {
        return Result.success(eventService.detail(id));
    }

    /** 5. 删除赛事（仅无报名记录、未开始赛事）：DELETE /api/event/delete/{id} */
    @ApiOperation("删除赛事（无报名且未开始）")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> delete(
            @ApiParam(value = "赛事ID", required = true) @PathVariable Long id) {
        return Result.success(eventService.delete(id));
    }

    /**
     * 6. 上传赛事封面图：POST /api/event/upload
     * 表单字段名：file
     * 校验：仅 jpg/jpeg/png，最大 5MB
     * 保存：本地 uploads 目录，文件名 = 时间戳 + 原后缀
     * 返回：{"url":"http://localhost:8080/uploads/xxx.jpg"}
     */
    @ApiOperation("上传赛事封面图")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @ApiParam(value = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.fail("图片大小不能超过 5MB");
        }

        String original = file.getOriginalFilename();
        String ext = extractExt(original);
        if (!isAllowedExt(ext)) {
            return Result.fail("仅支持 jpg / jpeg / png 格式");
        }

        // 按时间戳重命名，避免重名覆盖
        String newName = System.currentTimeMillis() + "." + ext;
        File dir = new File(uploadPath);
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.fail("创建上传目录失败");
        }
        File dest = new File(dir, newName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("文件保存失败：" + e.getMessage());
        }

        // 拼访问 URL（前端存到 coverImage 字段）
        String prefix = urlPrefix == null ? "/uploads/" : urlPrefix;
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        String url = "http://localhost:8080" + prefix + newName;

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("url", url);
        data.put("fileName", newName);
        return Result.success(data);
    }

    private static String extractExt(String filename) {
        if (filename == null) {
            return "";
        }
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedExt(String ext) {
        if (ext == null || ext.isEmpty()) {
            return false;
        }
        for (String a : ALLOWED_EXT) {
            if (a.equals(ext)) {
                return true;
            }
        }
        return false;
    }
}
