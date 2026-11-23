package dev.vladon.vesna.launcher.workspace

import dev.vladon.vesna.launcher.apps.LauncherApp

/**
 * Builds the bootstrap layout for a fresh install: the first apps (alphabetical
 * order, as produced by discovery) fill the dock, the next ones fill the first
 * workspace page.
 */
object SeedPlanner {

    fun plan(apps: List<LauncherApp>, grid: GridCanvas = GridCanvas.DEFAULT): List<WorkspaceItem> {
        val dock = apps.take(GridCanvas.DOCK_CAPACITY).mapIndexed { order, app ->
            WorkspaceItem(id = 0, key = app.key, container = Container.DOCK, cellIndex = order)
        }
        val workspace = apps.drop(GridCanvas.DOCK_CAPACITY).take(grid.capacity).mapIndexed { cell, app ->
            WorkspaceItem(id = 0, key = app.key, container = Container.WORKSPACE, cellIndex = cell)
        }
        return dock + workspace
    }
}
