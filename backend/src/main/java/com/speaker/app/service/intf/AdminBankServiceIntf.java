package com.speaker.app.service.intf;

import com.speaker.app.dto.AdminBankItemUpdateRequest;
import com.speaker.app.model.entity.QuestionBankItem;

import java.util.List;

public interface AdminBankServiceIntf {

    List<QuestionBankItem> listItems(String seasonLabel, String segment);

    void updateItem(long id, AdminBankItemUpdateRequest req);

    void softDelete(long id);

    int softDeleteBySeasonLabel(String seasonLabel);
}
