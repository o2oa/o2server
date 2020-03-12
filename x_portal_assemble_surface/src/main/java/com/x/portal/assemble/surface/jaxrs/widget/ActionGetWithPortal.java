package com.x.portal.assemble.surface.jaxrs.widget;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.bean.WrapCopier;
import com.x.base.core.project.bean.WrapCopierFactory;
import com.x.base.core.project.cache.ApplicationCache;
import com.x.base.core.project.exception.ExceptionWhen;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.portal.assemble.surface.Business;
import com.x.portal.assemble.surface.jaxrs.widget.ActionGetWithPortalMobile.Wo;
import com.x.portal.core.entity.Portal;
import com.x.portal.core.entity.Widget;
import com.x.portal.core.entity.Widget_;

import net.sf.ehcache.Element;

class ActionGetWithPortal extends BaseAction {

	ActionResult<Wo> execute(EffectivePerson effectivePerson, String flag, String portalFlag) throws Exception {
		ActionResult<Wo> result = new ActionResult<>();
		Wo wo = null;
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			Business business = new Business(emc);
			String cacheKey = ApplicationCache.concreteCacheKey(this.getClass(), flag, portalFlag);
			Element element = widgetCache.get(cacheKey);
			if ((null != element) && (null != element.getObjectValue())) {
				wo = (Wo) element.getObjectValue();
				Portal portal = business.portal().pick(wo.getPortal());
				if (!business.portal().visible(effectivePerson, portal)) {
					throw new ExceptionPortalAccessDenied(effectivePerson.getDistinguishedName(), portal.getName(),
							portal.getId());
				}
			} else {
				Portal portal = emc.flag(portalFlag, Portal.class);
				if (null == portal) {
					throw new ExceptionPortalNotExist(portalFlag);
				}
				if (!business.portal().visible(effectivePerson, portal)) {
					throw new ExceptionPortalAccessDenied(effectivePerson.getDistinguishedName(), portal.getName(),
							portal.getId());
				}
				Widget widget = emc.restrictFlag(flag, Widget.class, Widget.portal_FIELDNAME, portal.getId());
				if (null == widget) {
					throw new ExceptionWidgetNotExist(flag);
				}
				wo = Wo.copier.copy(widget);
				wo.setData(widget.getDataOrMobileData());
				widgetCache.put(new Element(cacheKey, wo));
			}
			result.setData(wo);
			return result;
		}
	}

	public static class Wo extends Widget {

		private static final long serialVersionUID = 3454132769791427909L;
		/** 不输出data数据,单独处理 */
		static WrapCopier<Widget, Wo> copier = WrapCopierFactory.wo(Widget.class, Wo.class,
				JpaObject.singularAttributeField(Widget.class, true, true), JpaObject.FieldsInvisible);

	}
}