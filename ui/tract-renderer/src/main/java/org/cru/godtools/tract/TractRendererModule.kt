package org.cru.godtools.tract

import androidx.lifecycle.ViewModel
import com.squareup.inject.assisted.dagger2.AssistedModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import org.ccci.gto.android.common.dagger.viewmodel.AssistedSavedStateViewModelFactory
import org.ccci.gto.android.common.dagger.viewmodel.ViewModelKey
import org.cru.godtools.tract.activity.ModalActivityDataModel
import org.cru.godtools.tract.activity.TractActivityDataModel
import org.cru.godtools.tract.liveshare.TractPublisherController
import org.cru.godtools.tract.liveshare.TractSubscriberController
import org.cru.godtools.tract.ui.tips.TipBottomSheetDialogFragmentDataModel
import org.greenrobot.eventbus.meta.SubscriberInfoIndex

@Module
@AssistedModule
@InstallIn(SingletonComponent::class)
abstract class TractRendererModule {
    @Binds
    @IntoMap
    @ViewModelKey(TractActivityDataModel::class)
    abstract fun tractActivityDataModel(f: TractActivityDataModel.Factory):
        AssistedSavedStateViewModelFactory<out ViewModel>

    @Binds
    @IntoMap
    @ViewModelKey(ModalActivityDataModel::class)
    internal abstract fun modalActivityDataModel(dataModel: ModalActivityDataModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TipBottomSheetDialogFragmentDataModel::class)
    internal abstract fun tipBottomSheetDialogDataModel(dataModel: TipBottomSheetDialogFragmentDataModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TractPublisherController::class)
    abstract fun tractPublisherController(f: TractPublisherController.Factory):
        AssistedSavedStateViewModelFactory<out ViewModel>

    @Binds
    @IntoMap
    @ViewModelKey(TractSubscriberController::class)
    abstract fun tractSubscriberController(controller: TractSubscriberController): ViewModel

    companion object {
        @IntoSet
        @Provides
        @Reusable
        fun tractEventBusIndex(): SubscriberInfoIndex = TractEventBusIndex()
    }
}
