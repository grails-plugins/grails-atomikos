grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'
grails.project.docs.output.dir = 'docs/manual' // for backwards-compatibility, the docs are checked into gh-pages branch

grails.project.dependency.resolution = {

	inherits 'global'

	log 'warn'

	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()

		mavenLocal()
		mavenCentral()
	}

	dependencies {
		compile('com.atomikos:atomikos-util:3.7.0')           { transitive = false }
		compile('com.atomikos:transactions:3.7.0')            { transitive = false }
		compile('com.atomikos:transactions-api:3.7.0')        { transitive = false }
		compile('com.atomikos:transactions-hibernate3:3.7.0') { transitive = false }
		compile('com.atomikos:transactions-jdbc:3.7.0')       { transitive = false }
		compile('com.atomikos:transactions-jta:3.7.0')        { transitive = false }
		compile('com.atomikos:transactions-jms:3.7.0')        { transitive = false }
	}

	plugins {
		build(':release:1.0.0.RC3') {
			export = false
		}
	}
}
