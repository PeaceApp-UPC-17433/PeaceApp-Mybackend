package com.upc.pre.peaceapp.community.domain.services;

import com.upc.pre.peaceapp.community.domain.model.aggregates.ReportComment;
import com.upc.pre.peaceapp.community.domain.model.commands.AddCommentCommand;
import com.upc.pre.peaceapp.community.domain.model.commands.DeleteCommentCommand;

public interface CommentCommandService {
    ReportComment handle(AddCommentCommand command);

    /** Solo el autor o un administrador. Si borra un administrador ajeno, queda en el historial de moderación. */
    void handle(DeleteCommentCommand command);
}
