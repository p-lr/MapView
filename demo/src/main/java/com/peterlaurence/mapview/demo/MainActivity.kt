package com.peterlaurence.mapview.demo

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationView
import com.peterlaurence.mapview.demo.fragments.MapAloneFragment
import com.peterlaurence.mapview.demo.fragments.MapMarkersFragment
import com.peterlaurence.mapview.demo.fragments.MapPathFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val fragmentTags = listOf(MAP_ALONE_TAG, MAP_MARKERS_TAG)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        if (savedInstanceState != null) {
            hideWelcomeMsg()
        }
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_map_alone -> {
                showMapAloneFragment()
            }
            R.id.nav_map_markers -> {
                showMapMarkersFragment()
            }
            R.id.nav_map_paths -> {
                showMapPathsFragment()
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showMapAloneFragment() {
        showFragment(MAP_ALONE_TAG) {
            createMapAloneFragment(it)
        }
    }

    private fun showMapMarkersFragment() {
        showFragment(MAP_MARKERS_TAG) {
            createMapMarkersFragment(it)
        }
    }

    private fun showMapPathsFragment() {
        showFragment(MAP_PATHS_TAG) {
            createMapPathsFragment(it)
        }
    }

    private fun showFragment(tag: String, onCreate: (t: FragmentTransaction) -> Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        removeFragments(tag)
        hideWelcomeMsg()
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: onCreate(transaction)
        transaction.show(fragment)
        transaction.commit()
    }

    private fun createMapAloneFragment(transaction: FragmentTransaction): Fragment {
        val fragment = MapAloneFragment()
        transaction.add(R.id.content_frame, fragment, MAP_ALONE_TAG)
        return fragment
    }

    private fun createMapMarkersFragment(transaction: FragmentTransaction): Fragment {
        val fragment = MapMarkersFragment()
        transaction.add(R.id.content_frame, fragment, MAP_MARKERS_TAG)
        return fragment
    }

    private fun createMapPathsFragment(transaction: FragmentTransaction): Fragment {
        val fragment = MapPathFragment()
        transaction.add(R.id.content_frame, fragment, MAP_PATHS_TAG)
        return fragment
    }

    private fun removeFragments(tagExcept: String) {
        val transaction = supportFragmentManager.beginTransaction()
        for (tag in fragmentTags) {
            if (tag == tagExcept) continue
            supportFragmentManager.findFragmentByTag(tag)?.also {
                transaction.remove(it)
            }
        }
        transaction.commit()
    }

    private fun hideWelcomeMsg() {
        val msg: TextView = findViewById(R.id.welcome_text)
        msg.visibility = View.GONE
    }
}

const val MAP_ALONE_TAG = "map_alone"
const val MAP_MARKERS_TAG = "map_markers"
const val MAP_PATHS_TAG = "map_paths"
