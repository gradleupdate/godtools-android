package org.cru.godtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import org.cru.godtools.databinding.FragmentGlobalDashboardBinding
import org.cru.godtools.viewmodel.GlobalDashboardDataModel
import java.util.Calendar

class GlobalDashboardFragment : BasePlatformFragment() {
    private var binding: FragmentGlobalDashboardBinding? = null
    private val viewModel: GlobalDashboardDataModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentGlobalDashboardBinding.inflate(inflater, container, false)
        binding?.viewModel = viewModel
        binding?.year = "${Calendar.getInstance().get(Calendar.YEAR)}"
        return binding?.root
    }
}
