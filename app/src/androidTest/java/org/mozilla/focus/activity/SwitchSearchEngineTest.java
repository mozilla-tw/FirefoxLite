/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Keep;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.autobot.MenuRobot;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.search.SearchEngine;
import org.mozilla.focus.search.SearchEngineManager;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

@Keep
@RunWith(AndroidJUnit4.class)
public class SwitchSearchEngineTest {

    @Rule
    public final GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private static final String SEARCH_KEYWORD = "zerda";
    private final List<SearchEngine> searchEngines = SearchEngineManager.getInstance().getSearchEngines();
    private SessionLoadedIdlingResource loadingIdlingResource;

    @Before
    public void setUp() {
        // Load mock search engines
        SearchEngineManager.getInstance().loadSearchEngines(InstrumentationRegistry.getContext());
        AndroidTestUtils.beforeTest();
    }

    @After
    public void tearDown() {
        if (loadingIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(loadingIdlingResource);
        }
    }

    /**
     * Test case no: TC_0004
     * Test case name: Change default search engine
     * Steps:
     * 1. Launch Rocket
     * 2. Tap on Menu and then Settings
     * 3. Tap on "Default search engine"
     * 4. Change a different search engine
     * 5. Back to home and search for something
     * 6. Search result is provided by selected search engine
     * 7. Repeat 2~6 for different search engines
     */

    @Ignore
    @Test
    public void switchSearchEngine_searchViaSearchEngineAccordingly() {
        activityTestRule.launchActivity(new Intent());
        final SearchEngine defaultSearchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(InstrumentationRegistry.getInstrumentation().getTargetContext());
        loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

        for (SearchEngine searchEngine : searchEngines) {
            switchSearchEngine(searchEngine);

            // Tap search field
            AndroidTestUtils.tapHomeSearchField();

            // Type search keyword and browse
            AndroidTestUtils.typeTextInSearchFieldAndGo(SEARCH_KEYWORD);

            IdlingRegistry.getInstance().register(loadingIdlingResource);

            // Check is url contains search keyword
            AndroidTestUtils.urlBarContainsText(SEARCH_KEYWORD);

            // Check is url contains search engine host name
            AndroidTestUtils.urlBarContainsText(getHostName(searchEngine.getSearchForm()));

            IdlingRegistry.getInstance().unregister(loadingIdlingResource);

            // Remove tab and back to home
            AndroidTestUtils.removeNewAddedTab();
        }

        // Restore default search engine setting
        switchSearchEngine(defaultSearchEngine);

    }

    private void switchSearchEngine(SearchEngine engine) {

        final SearchEngine currentSearchEngine = SearchEngineManager.getInstance().getDefaultSearchEngine(InstrumentationRegistry.getInstrumentation().getTargetContext());
        // When target search engine and current search engine is the same, skip switch

        if (TextUtils.equals(currentSearchEngine.getName(), engine.getName())) {
            return;
        }

        final String[] searchEngineName = engine.getName().split(" ");

        MenuRobot menuRobot = new MenuRobot();
        // Open menu
        menuRobot.clickHomeMenu();

        // Open settings
        menuRobot.clickMenuSettings();

        // Open default search engine setting
        onView(withText(R.string.preference_search_engine_default)).check(matches(isDisplayed())).perform(click());

        // Set target search engine as default
        onView(withText(containsString(searchEngineName[0]))).check(matches(isDisplayed())).perform(click());

        // Check setting is showed again
        onView(withText(R.string.preference_search_engine_default)).check(matches(isDisplayed()));

        // Back to home
        Espresso.pressBack();
    }

    private String getHostName(String searchForm) {
        Uri uri = Uri.parse(searchForm);

        if (uri.getHost() == null) {
            throw new AssertionError("The given searchForm doesn't contains a valid host " + searchForm);
        }

        String hostname = uri.getHost();
        // return only hostname, without www.
        return hostname.startsWith("www.") ? hostname.substring(4) : hostname;
    }
}