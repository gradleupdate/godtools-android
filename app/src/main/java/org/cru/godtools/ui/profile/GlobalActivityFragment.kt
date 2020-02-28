package org.cru.godtools.ui.profile

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import org.ccci.gto.android.common.db.findLiveData
import org.cru.godtools.R
import org.cru.godtools.databinding.ProfilePageGlobalActivityFragmentBinding
import org.cru.godtools.fragment.BaseBindingPlatformFragment
import org.cru.godtools.model.GlobalActivityAnalytics
import org.cru.godtools.sync.syncGlobalAnalytics
import org.keynote.godtools.android.db.GodToolsDao
import java.util.Calendar

class GlobalActivityFragment : BaseBindingPlatformFragment<ProfilePageGlobalActivityFragmentBinding>(
    R.layout.profile_page_global_activity_fragment
) {
    private val viewModel: GlobalActivityFragmentViewModel by viewModels()

    override fun onBindingCreated(binding: ProfilePageGlobalActivityFragmentBinding, savedInstanceState: Bundle?) {
        binding.globalActivity = viewModel.globalActivity
        binding.year = "${Calendar.getInstance().get(Calendar.YEAR)}"
    }

    override fun syncData(force: Boolean) {
        super.syncData(force)
        syncHelper.sync(syncGlobalAnalytics(requireContext(), force))
    }
}

class GlobalActivityFragmentViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = GodToolsDao.getInstance(application)
    val globalActivity = dao.findLiveData<GlobalActivityAnalytics>(1)
}
