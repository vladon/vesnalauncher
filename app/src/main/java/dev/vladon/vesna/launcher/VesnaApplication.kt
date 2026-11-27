package dev.vladon.vesna.launcher

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import dev.vladon.vesna.launcher.data.FolderDao
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.notifications.NotificationBadges
import dev.vladon.vesna.launcher.platform.AppDiscovery
import dev.vladon.vesna.launcher.platform.AppLauncher
import dev.vladon.vesna.launcher.platform.HomeRoleChecker
import dev.vladon.vesna.launcher.platform.IconPackResolver
import dev.vladon.vesna.launcher.platform.LauncherAppsAppDiscovery
import dev.vladon.vesna.launcher.platform.PackageChangeMonitor
import dev.vladon.vesna.launcher.settings.AppearancePreferences
import dev.vladon.vesna.launcher.workspace.LauncherController
import dev.vladon.vesna.launcher.workspace.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph of the launcher. Deliberately tiny: a DI framework would
 * not pay for itself with this few singletons.
 */
class LauncherGraph(appContext: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: LauncherDatabase = LauncherDatabase.build(appContext)
    val widgetDao: dev.vladon.vesna.launcher.data.WidgetDao = database.widgetDao()

    val workspaceRepository: WorkspaceRepository =
        WorkspaceRepository(
            dao = database.workspaceItemDao(),
            db = database,
            folderDao = database.folderDao(),
            prefs = appContext.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE),
        )

    val discovery: AppDiscovery =
        LauncherAppsAppDiscovery(appContext, IconPackResolver(appContext, AppearancePreferences(appContext)))

    val appLauncher: AppLauncher = AppLauncher(appContext)

    val packageChangeMonitor: PackageChangeMonitor = PackageChangeMonitor(appContext)

    val homeRoleChecker: HomeRoleChecker = HomeRoleChecker(appContext)

    val controller: LauncherController = LauncherController(
        scope = applicationScope,
        discovery = discovery,
        appLauncher = appLauncher,
        workspaceRepository = workspaceRepository,
        widgetDao = widgetDao,
        packageEvents = packageChangeMonitor.events,
    )
}

class VesnaApplication : Application() {

    lateinit var graph: LauncherGraph
        private set
    private lateinit var appLocales: LocaleList

    override fun onCreate() {
        super.onCreate()
        appLocales = resources.configuration.locales
        graph = LauncherGraph(this)
        NotificationBadges.init(this)
        // Registered for the whole process lifetime, which is how launchers observe
        // install and uninstall storms without restarting.
        graph.packageChangeMonitor.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val locales = newConfig.locales
        if (locales == appLocales) return
        appLocales = locales
        graph.controller.refreshLocalizedApps()
    }

    companion object {
        fun graph(context: Context): LauncherGraph =
            (context.applicationContext as VesnaApplication).graph
    }
}
