package com.speaker.app.service.intf;

import com.speaker.app.dto.AdminImportMarkdownRequest;
import com.speaker.app.dto.AdminImportMarkdownResponse;
import com.speaker.app.dto.AdminPreviewMarkdownResponse;

public interface AdminBankImportServiceIntf {

    AdminPreviewMarkdownResponse preview(AdminImportMarkdownRequest req);

    AdminImportMarkdownResponse importMarkdown(AdminImportMarkdownRequest req);
}
