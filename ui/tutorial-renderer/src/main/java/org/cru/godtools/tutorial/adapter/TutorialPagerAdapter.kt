package org.cru.godtools.tutorial.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.viewpager.widget.PagerAdapter
import org.cru.godtools.tutorial.BR
import org.cru.godtools.tutorial.Page
import org.cru.godtools.tutorial.R
import org.cru.godtools.tutorial.animation.animateToNextText
import org.cru.godtools.tutorial.databinding.BakedInTutorialWelcomeBinding
import org.cru.godtools.tutorial.util.TutorialCallbacks

internal class TutorialPagerAdapter(private val pages: List<Page>, val callbacks: TutorialCallbacks) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(
            LayoutInflater.from(container.context),
            pages[position].layout,
            container,
            false
        ).also { it.setVariable(BR.callback, callbacks) }
        container.addView(binding.root)
        binding.setDataBindingAnimation()
        return binding.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun getCount(): Int = pages.size

    private fun ViewDataBinding.setDataBindingAnimation() {
        when (this) {
            is BakedInTutorialWelcomeBinding -> welcomeTextView.animateToNextText(R.string.baked_in_welcome_helping)
        }
    }
}
