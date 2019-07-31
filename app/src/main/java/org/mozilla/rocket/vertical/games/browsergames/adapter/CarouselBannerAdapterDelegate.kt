package org.mozilla.rocket.vertical.games.browsergames.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_carousel_banner.carousel_list
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.vertical.games.GamesViewModel
import org.mozilla.rocket.vertical.games.browsergames.CarouselBannerAdapter

class CarouselBannerAdapterDelegate(private val gamesViewModel: GamesViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            CarouselBannerViewHolder(view, gamesViewModel)
}

class CarouselBannerViewHolder(override val containerView: View, viewModel: GamesViewModel) : DelegateAdapter.ViewHolder(containerView) {
    private var adapter = CarouselBannerAdapter(viewModel)

    init {
        carousel_list.adapter = this@CarouselBannerViewHolder.adapter
    }

    override fun bind(uiModel: DelegateAdapter.UIModel) {
        uiModel as CarouselBanner
        adapter.setData(uiModel.banners)
    }
}

data class CarouselBanner(val banners: List<CarouselBannerAdapter.BannerItem>) : DelegateAdapter.UIModel()