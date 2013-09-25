grails.project.work.dir = 'target'
grails.project.source.level = 1.6
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch

grails.project.dependency.resolver = 'maven'
grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		compile('com.atomikos:atomikos-util:3.8.0') {
			excludes 'servlet-api', 'slf4j-api', 'junit', 'log4j', 'mockito-all', 'geronimo-jta_1.0.1B_spec'
		}
		compile('com.atomikos:transactions:3.8.0') {
			excludes 'mockito-all', 'junit', 'geronimo-jta_1.0.1B_spec'
		}
		compile('com.atomikos:transactions-api:3.8.0') {
			excludes 'mockito-all', 'junit', 'geronimo-jta_1.0.1B_spec'
		}
		compile('com.atomikos:transactions-hibernate3:3.8.0') {
			excludes 'hibernate', 'slf4j-api', 'slf4j-simple', 'mockito-all', 'junit', 'geronimo-jta_1.0.1B_spec'
		}
		compile('com.atomikos:transactions-jdbc:3.8.0') {
			excludes 'mockito-all', 'junit', 'geronimo-jta_1.0.1B_spec'
		}
		compile('com.atomikos:transactions-jms:3.8.0') {
			excludes 'geronimo-jms_1.1_spec', 'slf4j-simple', 'mockito-all', 'junit', 'geronimo-jta_1.0.1B_spec'
		}
		compile('com.atomikos:transactions-jta:3.8.0') {
			excludes 'geronimo-jms_1.1_spec', 'geronimo-j2ee-connector_1.5_spec',
						'mockito-all', 'junit', 'geronimo-jta_1.0.1B_spec'
		}
		compile('org.apache.geronimo.specs:geronimo-j2ee-management_1.1_spec:1.0.1') {
			excludes 'geronimo-ejb_3.0_spec', 'junit', 'logging-config'
		}
		compile('org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.1.1') {
			excludes 'mockobjects-core', 'mockobjects-jdk1.4-j2ee1.3', 'junit', 'logging-config'
		}
		compile 'org.springframework:spring-jms:3.2.4.RELEASE'
	}

	plugins {
		compile(":hibernate:3.6.10.1") {
			export = false
		}

		build(':release:2.0.4', ':rest-client-builder:1.0.2') {
			export = false
		}
	}
}
