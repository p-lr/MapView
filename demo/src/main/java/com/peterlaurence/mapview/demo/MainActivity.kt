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
import com.peterlaurence.mapview.demo.fragments.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val fragmentTags = listOf(MAP_ALONE_TAG, MAP_MARKERS_TAG, MAP_PATHS_TAG,
            MAP_DEFERRED_TAG, MAP_REMOTE_HTTP_TAG, MAP_ROTATING_TAG)

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
            removeFragments("")
            showWelcomeMsg()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_map_alone -> createAndShowFragment<MapAloneFragment>(MAP_ALONE_TAG)
            R.id.nav_map_markers -> createAndShowFragment<MapMarkersFragment>(MAP_MARKERS_TAG)
            R.id.nav_map_paths -> createAndShowFragment<MapPathFragment>(MAP_PATHS_TAG)
            R.id.nav_remote_http -> createAndShowFragment<RemoteHttpFragment>(MAP_REMOTE_HTTP_TAG)
            R.id.nav_map_deferred_configuration -> createAndShowFragment<DeferredFragment>(MAP_DEFERRED_TAG)
            R.id.nav_map_rotating -> createAndShowFragment<RotatingMapFragment>(MAP_ROTATING_TAG)
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private inline fun <reified T : Fragment> createAndShowFragment(tag: String) {
        showFragment(tag) { tr, _ ->
            createFragment(tr, T::class.java, tag)
        }
    }

    private fun showFragment(tag: String, onCreate: (t: FragmentTransaction, tag: String) -> Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        removeFragments(tag)
        hideWelcomeMsg()
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: onCreate(transaction, tag)
        transaction.show(fragment)
        transaction.commit()
    }

    private fun <T : Fragment> createFragment(transaction: FragmentTransaction, clazz: Class<T>, tag: String): Fragment {
        val f = clazz.newInstance()
        transaction.add(R.id.content_frame, f, tag)
        return f
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

    private fun showWelcomeMsg() {
        val msg: TextView = findViewById(R.id.welcome_text)
        msg.visibility = View.VISIBLE
    }
}

const val MAP_ALONE_TAG = "map_alone"
const val MAP_MARKERS_TAG = "map_markers"
const val MAP_PATHS_TAG = "map_paths"
const val MAP_REMOTE_HTTP_TAG = "map_remote_http"
const val MAP_DEFERRED_TAG = "map_deferred"
const val MAP_ROTATING_TAG = "map_rotating"
