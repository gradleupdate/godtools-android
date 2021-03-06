package org.cru.godtools.base.tool

import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.ccci.gto.android.common.dagger.viewmodel.ViewModelKey
import org.cru.godtools.base.tool.activity.BaseSingleToolActivityDataModel
import org.cru.godtools.base.tool.viewmodel.LatestPublishedManifestDataModel

@Module
abstract class BaseToolRendererModule {
    @Binds
    @IntoMap
    @ViewModelKey(BaseSingleToolActivityDataModel::class)
    internal abstract fun baseSingleToolActivityDataModel(dataModel: BaseSingleToolActivityDataModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LatestPublishedManifestDataModel::class)
    internal abstract fun latestPublishedManifestDataModel(dataModel: LatestPublishedManifestDataModel): ViewModel
}
