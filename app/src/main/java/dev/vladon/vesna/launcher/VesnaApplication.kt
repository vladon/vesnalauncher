package dev.vladon.vesna.launcher

import android.app.Application
import android.content.Context
import dev.vladon.vesna.launcher.data.LauncherDatabase
import dev.vladon.vesna.launcher.platform.AppDiscovery
import dev.vladon.vesna.launcher.platform.AppLauncher
import dev.vladon.vesna.launcher.platform.HomeRoleChecker
import dev.vladon.vesna.launcher.platform.PackageChangeMonitor
import dev.vladon.vesna.launcher.platform.PackageManagerAppDiscovery
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

    val workspaceRepository: WorkspaceRepository =
        WorkspaceRepository(
            dao = database.workspaceItemDao(),
            db = database,
            prefs = appContext.getSharedPreferences("vesna_launcher", Context.MODE_PRIVATE),
        )

    val discovery: AppDiscovery = PackageManagerAppDiscovery(appContext)

    val appLauncher: AppLauncher = AppLauncher(appContext)

    val packageChangeMonitor: PackageChangeMonitor = PackageChangeMonitor(appContext)

    val homeRoleChecker: HomeRoleChecker = HomeRoleChecker(appContext)

    val controller: LauncherController = LauncherController(
        scope = applicationScope,
        discovery = discovery,
        appLauncher = appLauncher,
        workspaceRepository = workspaceRepository,
        packageEvents = packageChangeMonitor.events,
    )
}

class VesnaApplication : Application() {

    lateinit var graph: LauncherGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = LauncherGraph(this)
        // Registered for the whole process lifetime, which is how launchers observe
        // install and uninstall storms without restarting.
        graph.packageChangeMonitor.start()
    }

    companion object {
        fun graph(context: Context): LauncherGraph =
            (context.applicationContext as VesnaApplication).graph
    }
}
