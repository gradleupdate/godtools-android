package org.keynote.godtools.android.content;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.ccci.gto.android.common.eventbus.content.DaoCursorEventBusLoader;
import org.cru.godtools.model.Tool;
import org.cru.godtools.model.event.content.ToolEventBusSubscriber;
import org.keynote.godtools.android.db.GodToolsDao;

public class ToolsCursorLoader extends DaoCursorEventBusLoader<Tool> {
    public ToolsCursorLoader(@NonNull final Context context, @Nullable final Bundle args) {
        super(context, GodToolsDao.getInstance(context), Tool.class, args);
        addEventBusSubscriber(new ToolEventBusSubscriber(this));
    }
}
