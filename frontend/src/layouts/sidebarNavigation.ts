import { isDetailParent } from '@/layouts/roleSidebar'
import type { RoleSidebarConfig, RoleSidebarItem } from '@/layouts/roleSidebar'

export interface SidebarRouteMatch {
  item: RoleSidebarItem
  parent?: {
    groupIndex: number
    itemIndex: number
  }
}

function matchesSidebarRoute(item: RoleSidebarItem, path: string) {
  return item.to === path || item.matchPrefixes?.some((prefix) => path.startsWith(prefix))
}

export function resolveSidebarRoute(sidebar: RoleSidebarConfig, path: string): SidebarRouteMatch | undefined {
  for (const [groupIndex, group] of sidebar.detailGroups.entries()) {
    for (const [itemIndex, item] of group.items.entries()) {
      if (isDetailParent(item)) {
        const child = item.children.find((candidate) => matchesSidebarRoute(candidate, path))
        if (child) return { item: child, parent: { groupIndex, itemIndex } }
      } else if (matchesSidebarRoute(item, path)) {
        return { item }
      }
    }
  }
}
