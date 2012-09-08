/* Copyright 2011-2012 SpringSource.
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

import grails.plugin.atomikos.AtomikosBeanPostProcessor
import grails.plugin.atomikos.UserTransactionServiceProperties

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.listener.DefaultMessageListenerContainer
import org.springframework.transaction.jta.JtaTransactionManager

import com.atomikos.icatch.config.UserTransactionServiceImp
import com.atomikos.icatch.jta.J2eeUserTransaction
import com.atomikos.icatch.jta.UserTransactionManager
import com.atomikos.icatch.standalone.UserTransactionServiceFactory

class AtomikosGrailsPlugin {

	private Logger log = LoggerFactory.getLogger('grails.plugin.atomikos.AtomikosGrailsPlugin')

	String version = '1.0'
	String grailsVersion = '1.3 > *'	
	String author = 'Burt Beckwith'
	String authorEmail = 'beckwithb@vmware.com'
	String title = 'Atomikos JTA/XA Plugin'
	String description = 'Integrates Atomikos TransactionsEssentials to support two-phase commit for JDBC and JMS transactions'
	String documentation = 'http://grails.org/plugin/atomikos'

	def loadAfter = ['dataSource', 'domainClass', 'hibernate']
	def pluginExcludes = [
		'docs/**',
		'src/docs/**'
	]

	String license = 'APACHE'
	def organization = [name: 'SpringSource', url: 'http://www.springsource.org/']
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPATOMIKOS']
	def scm = [url: 'https://github.com/grails-plugins/grails-atomikos']

	def doWithSpring = {

		System.setProperty UserTransactionServiceImp.NO_FILE_PROPERTY_NAME, 'true'
		System.setProperty 'com.atomikos.icatch.service', UserTransactionServiceFactory.name

		atomikosUserTransactionServiceProperties(UserTransactionServiceProperties) {
			application = application
		}

		atomikosUserTransactionService(UserTransactionServiceImp, ref('atomikosUserTransactionServiceProperties')) { bean ->
			bean.initMethod = 'init'
			bean.destroyMethod = 'shutdownForce'
		}

		atomikosTransactionManager(UserTransactionManager) { bean ->
			bean.initMethod = 'init'
			bean.destroyMethod = 'close'
			bean.dependsOn = 'atomikosUserTransactionService'
			forceShutdown = false
		}
  
		atomikosUserTransaction(J2eeUserTransaction) { bean ->
			bean.dependsOn = 'atomikosUserTransactionService'
		}

		transactionManager(JtaTransactionManager) {
			transactionManager = ref('atomikosTransactionManager')
			userTransaction = ref('atomikosUserTransaction')
			allowCustomIsolationLevels = true
		}

		atomikosBeanPostProcessor(AtomikosBeanPostProcessor) {
			grailsApplication = application
		}

		if (manager.hasGrailsPlugin('jms')) {

			// application needs to define an XA jmsConnectionFactory, e.g.
			/*
			jmsConnectionFactory(org.apache.activemq.ActiveMQXAConnectionFactory) {
				brokerURL = 'vm://localhost'
			}
			*/

			atomikosJmsConnectionFactory(com.atomikos.jms.AtomikosConnectionFactoryBean) { bean ->
				bean.initMethod = 'init'
				uniqueResourceName = 'atomikosJmsConnectionFactory'
				xaConnectionFactory = ref('jmsConnectionFactory')
				maxPoolSize = 10
//				minPoolSize = 1
//				borrowConnectionTimeout = 30
//				maintenanceInterval = 60
//				maxIdleTime = 60
//				reapTimeout = 0
			}
		}
	}

	def doWithApplicationContext = { ctx ->

		def conf = application.config.grails.plugin.atomikos

		def toBoolean = { value, boolean defaultIfMissing -> value instanceof Boolean ? value : defaultIfMissing }

		for (entry in ctx.getBeansOfType(JmsTemplate)) {
			String name = entry.key
			if (!toBoolean(conf.convert[name], true)) {
				log.debug "Not converting JmsTemplate $name to XA"
				continue
			}

			entry.value.connectionFactory = ctx.atomikosJmsConnectionFactory
			entry.value.sessionTransacted = true
			log.debug "Converted JmsTemplate $name to XA"
		}

		for (entry in ctx.getBeansOfType(DefaultMessageListenerContainer)) {
			String name = entry.key
			if (!toBoolean(conf.convert[name], true)) {
				log.debug "Not converting DefaultMessageListenerContainer $name to XA"
				continue
			}

			def bean = entry.value

			// configure XA
			bean.transactionManager = ctx.transactionManager
			bean.connectionFactory = ctx.atomikosJmsConnectionFactory
			bean.sessionTransacted = true

			if (bean.messageListener.getClass().name == 'grails.plugin.jms.listener.adapter.PersistenceContextAwareListenerAdapter') {
				// not needed since we'll be in a TX already
				bean.messageListener.persistenceInterceptor = null
			}

			log.debug "Converted DefaultMessageListenerContainer $name to XA"
		}
	}
}
