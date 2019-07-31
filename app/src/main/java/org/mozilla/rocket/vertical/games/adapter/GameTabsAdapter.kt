package org.mozilla.rocket.vertical.games.adapter

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mozilla.focus.R
import org.mozilla.rocket.vertical.games.BrowserGamesFragment
import org.mozilla.rocket.vertical.games.GamesActivity

@SuppressLint("WrongConstant")
class GameTabsAdapter(fm: FragmentManager, activity: FragmentActivity) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val resource = activity.resources

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> BrowserGamesFragment()
            1 -> GamesActivity.PremiumGamesFragment()
            else -> throw IndexOutOfBoundsException("position: $position")
        }
    }

    override fun getCount(): Int = GAMES_TAB_COUNT

    override fun getPageTitle(position: Int): CharSequence? {
        val resId = when (position) {
            0 -> R.string.games_tab_title_browser_games
            1 -> R.string.games_tab_title_premium_games
            else -> throw IndexOutOfBoundsException("position: $position")
        }

        return resource.getString(resId)
    }

    companion object {
        const val GAMES_TAB_COUNT = 2
    }
}