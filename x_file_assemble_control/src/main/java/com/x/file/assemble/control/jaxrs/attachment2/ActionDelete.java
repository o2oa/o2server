package com.x.file.assemble.control.jaxrs.attachment2;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.annotation.CheckPersistType;
import com.x.base.core.project.config.StorageMapping;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.jaxrs.WoId;
import com.x.base.core.project.jaxrs.WrapBoolean;
import com.x.base.core.project.tools.ListTools;
import com.x.file.assemble.control.Business;
import com.x.file.assemble.control.ThisApplication;
import com.x.file.core.entity.personal.Attachment;
import com.x.file.core.entity.personal.Attachment2;
import com.x.file.core.entity.personal.Recycle;
import com.x.file.core.entity.personal.Share;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityManager;

class ActionDelete extends BaseAction {

	ActionResult<Wo> execute(EffectivePerson effectivePerson, String id) throws Exception {
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			ActionResult<Wo> result = new ActionResult<>();
			Business business = new Business(emc);
			Attachment2 attachment = emc.find(id, Attachment2.class);
			if (null == attachment) {
				throw new ExceptionAttachmentNotExist(id);
			}
			if (!effectivePerson.isManager() && !StringUtils.equals(effectivePerson.getDistinguishedName(), attachment.getPerson())) {
				throw new ExceptionAttachmentAccessDenied(effectivePerson, attachment);
			}
			Share share = business.share().getShareByFileId(attachment.getId(),effectivePerson.getDistinguishedName());
			if(share!=null){
				EntityManager sem = emc.beginTransaction(Share.class);
				sem.remove(share);
				sem.getTransaction().commit();
			}
			if("正常".equals(attachment.getStatus())){
				Recycle recycle = new Recycle(attachment.getPerson(), attachment.getName(), attachment.getId(), "attachment");
				recycle.setExtension(attachment.getExtension());
				recycle.setLength(attachment.getLength());
				EntityManager rem = emc.beginTransaction(Recycle.class);
				rem.persist(recycle);
				rem.getTransaction().commit();

				EntityManager aem = emc.beginTransaction(Attachment2.class);
				attachment.setStatus("已删除");
				aem.getTransaction().commit();
			}
			Wo wo = new Wo();
			wo.setValue(true);
			result.setData(wo);
			return result;
		}
	}

	public static class Wo extends WrapBoolean {

	}
}
