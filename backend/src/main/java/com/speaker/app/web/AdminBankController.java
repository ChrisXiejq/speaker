package com.speaker.app.web;

import com.speaker.app.dto.AdminBankItemUpdateRequest;
import com.speaker.app.dto.AdminImportMarkdownRequest;
import com.speaker.app.dto.AdminImportMarkdownResponse;
import com.speaker.app.dto.AdminPreviewMarkdownResponse;
import com.speaker.app.model.entity.QuestionBankItem;
import com.speaker.app.service.AdminBankCrudService;
import com.speaker.app.service.AdminBankImportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bank")
public class AdminBankController {

    private final AdminBankImportService adminBankImportService;
    private final AdminBankCrudService adminBankCrudService;

    public AdminBankController(AdminBankImportService adminBankImportService, AdminBankCrudService adminBankCrudService) {
        this.adminBankImportService = adminBankImportService;
        this.adminBankCrudService = adminBankCrudService;
    }

    @GetMapping("/items")
    public List<QuestionBankItem> listItems(
            @RequestParam String seasonLabel,
            @RequestParam String segment) {
        return adminBankCrudService.listItems(seasonLabel, segment);
    }

    @PutMapping("/items/{id}")
    public void updateItem(@PathVariable long id, @Valid @RequestBody AdminBankItemUpdateRequest body) {
        adminBankCrudService.updateItem(id, body);
    }

    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable long id) {
        adminBankCrudService.softDelete(id);
    }

    /** 软删除指定季节下全部题目 */
    @DeleteMapping("/items/by-season")
    public Map<String, Integer> softDeleteSeason(@RequestParam String seasonLabel) {
        int n = adminBankCrudService.softDeleteBySeasonLabel(seasonLabel);
        return Map.of("softDeleted", n);
    }

    @PostMapping("/preview-markdown")
    public AdminPreviewMarkdownResponse previewMarkdown(@Valid @RequestBody AdminImportMarkdownRequest body) {
        return adminBankImportService.preview(body);
    }

    @PostMapping("/import-markdown")
    public AdminImportMarkdownResponse importMarkdown(@Valid @RequestBody AdminImportMarkdownRequest body) {
        return adminBankImportService.importMarkdown(body);
    }
}
