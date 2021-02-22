package ovh.plrapps.mapview.demo

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val navController: NavController by lazy {
        findNavController(R.id.nav_host_fragment)
    }

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
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_map_alone -> showMapAlone()
            R.id.nav_map_constrained -> showMapConstrained()
            R.id.nav_map_markers -> showMapMarkers()
            R.id.nav_map_paths -> showMapPath()
            R.id.nav_remote_http -> showRemoteHttp()
            R.id.nav_map_deferred_configuration -> showDeferred()
            R.id.nav_map_rotating -> showRotating()
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showMapAlone() {
        if (getString(R.string.fragment_map_alone) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalMapAloneFragment()
            navController.navigate(action)
        }
    }

    private fun showMapConstrained() {
        if (getString(R.string.fragment_map_constrained) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalMapConstrainedFragment()
            navController.navigate(action)
        }
    }

    private fun showMapMarkers() {
        if (getString(R.string.fragment_map_markers) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalMapMarkersFragment()
            navController.navigate(action)
        }
    }

    private fun showMapPath() {
        if (getString(R.string.fragment_map_path) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalMapPathFragment()
            navController.navigate(action)
        }
    }

    private fun showRemoteHttp() {
        if (getString(R.string.fragment_remote_http) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalRemoteHttpFragment()
            navController.navigate(action)
        }
    }

    private fun showDeferred() {
        if (getString(R.string.fragment_deferred) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalDeferredFragment()
            navController.navigate(action)
        }
    }

    private fun showRotating() {
        if (getString(R.string.fragment_rotating) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalRotatingMapFragment()
            navController.navigate(action)
        }
    }
}
