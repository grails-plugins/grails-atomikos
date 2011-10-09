/* Copyright 2011 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.atomikos

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.GenericBeanDefinition

import com.atomikos.icatch.jta.hibernate3.AtomikosJTATransactionFactory
import com.atomikos.icatch.jta.hibernate3.TransactionManagerLookup
import com.atomikos.jdbc.AtomikosDataSourceBean

/**
 * Replaces datasource beans with XA beans.
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class AtomikosBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private Logger log = LoggerFactory.getLogger(getClass())

	GrailsApplication grailsApplication

	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

		def toBoolean = { value, boolean defaultIfMissing -> value instanceof Boolean ? value : defaultIfMissing }

		def conf = grailsApplication.config.grails.plugin.atomikos

		for (String name in registry.getBeanDefinitionNames()) {
			if (name.startsWith('dataSource')) {
				if (toBoolean(conf.convert[name], true)) {
					convertDataSourceToXa registry, name
				}
				else {
					log.debug "Not converting DataSource $name to XA"
				}
			}
		}

		if (!grailsApplication.config.grails.plugin.atomikos.containsKey('uts')) {
			grailsApplication.config.grails.plugin.atomikos.uts = [
				'com.atomikos.icatch.console_file_name': 'tm.out',
				'com.atomikos.icatch.log_base_name': 'tmlog',
				'com.atomikos.icatch.tm_unique_name': 'atomikosTransactionManager',
				'com.atomikos.icatch.console_log_level': 'INFO',
				'com.atomikos.icatch.log_base_dir': 'target/atomikos',
				'com.atomikos.icatch.output_dir': 'target/atomikos',
				'com.atomikos.icatch.force_shutdown_on_vm_exit': 'true']
		}
	}

	protected void convertDataSourceToXa(BeanDefinitionRegistry registry, String beanName) {

		def xaConfig = grailsApplication.config[beanName].xaConfig
		if (!xaConfig) {
			log.debug "DataSource $beanName has no XA config"
			return
		}

		xaConfig = [:] + xaConfig
		xaConfig.xaDataSourceClassName = xaConfig.remove('driverClassName')
		xaConfig.xaProperties = xaConfig.remove('driverProperties')
		xaConfig.uniqueResourceName = beanName

		registry.removeBeanDefinition beanName
		registry.registerBeanDefinition beanName, new GenericBeanDefinition(
			beanClass: AtomikosDataSourceBean,
			initMethodName: 'init',
			destroyMethodName: 'close',
			propertyValues: new MutablePropertyValues(xaConfig))

		updateHibernateProperties registry, beanName

		updateTransactionManger registry, beanName

		log.debug "Configured DataSource '$beanName' with XA config $xaConfig"
	}

	protected void updateHibernateProperties(BeanDefinitionRegistry registry, String beanName) {
		String propertiesName = 'hibernateProperties' + (beanName - 'dataSource')
		if (!registry.containsBeanDefinition(propertiesName)) {
			return
		}

		Map props = registry.getBeanDefinition(propertiesName).propertyValues.propertyValueList[0].value
		props['hibernate.transaction.factory_class'] = AtomikosJTATransactionFactory.name
		props['hibernate.transaction.manager_lookup_class'] = TransactionManagerLookup.name

		registry.removeBeanDefinition propertiesName
		registry.registerBeanDefinition propertiesName, new GenericBeanDefinition(
			beanClass: PropertiesFactoryBean,
			scope: 'prototype',
			propertyValues: new MutablePropertyValues([properties: props]))

		log.debug "Configured '$propertiesName' for XA: $props"
	}

	protected void updateTransactionManger(BeanDefinitionRegistry registry, String beanName) {
		if (beanName == 'dataSource') {
			// don't mess with the real transactionManager bean
			return
		}

		String txManagerName = 'transactionManager' + (beanName - 'dataSource')
		if (!registry.containsBeanDefinition(txManagerName)) {
			return
		}

		// make sure everyone uses the one XA tx manager
		registry.removeBeanDefinition txManagerName
		registry.registerAlias 'transactionManager', txManagerName

		log.debug "Registered '$txManagerName' as an alias for 'transactionManager'"
	}

	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// not used
	}
}
