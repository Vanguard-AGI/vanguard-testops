package io.vanguard.testops.context;

import io.vanguard.testops.provider.BaseAssociateCaseProvider;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 关联用例接口Bean实例上下文
 * 主应用未扫描 io.vanguard.testops.context，本类可能不是 Bean，setApplicationContext 可能未被调用；
 * getInstance 先查 PROVIDER_MAP（单测），再查本类缓存的 context（含父上下文），最后用 CommonBeanFactory 的 context 查找。
 */
@Component
public class AssociateCaseFactory implements ApplicationContextAware {

	/** 仅供单测注入 mock，生产不写 */
	public static final Map<String, BaseAssociateCaseProvider> PROVIDER_MAP = new HashMap<>();

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		applicationContext = context;
	}

	public static BaseAssociateCaseProvider getInstance(String serviceType) {
		BaseAssociateCaseProvider fromMap = PROVIDER_MAP.get(serviceType);
		if (fromMap != null) {
			return fromMap;
		}
		// 本类若未被扫描为 Bean，applicationContext 为 null，用 CommonBeanFactory（sdk 包已扫描）的 context 查找
		if (applicationContext != null) {
			Map<String, BaseAssociateCaseProvider> beanMap = BeanFactoryUtils.beansOfTypeIncludingAncestors(
					applicationContext, BaseAssociateCaseProvider.class);
			BaseAssociateCaseProvider p = beanMap.get(serviceType);
			if (p != null) {
				return p;
			}
		}
		try {
			Map<String, BaseAssociateCaseProvider> beanMap = CommonBeanFactory.getBeansOfType(BaseAssociateCaseProvider.class);
			return beanMap != null ? beanMap.get(serviceType) : null;
		} catch (Exception e) {
			return null;
		}
	}
}
