package com.zhongruan.edu.biz.shared.security.scope;

import com.zhongruan.edu.common.context.RequestContext;

public interface ResourceScopeAuthorizer {
    boolean hasAccess(RequestContext context, ResourceType resourceType, Long resourceId, ResourceAction action);
}

