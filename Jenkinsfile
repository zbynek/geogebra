pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                sh label: 'test', script: "./gradlew :common-jre:test :common-jre:jacocoTestReport"
            }
        }
        stage('reports') {
            steps {
                publishCoverage adapters: [jacocoAdapter('**/build/reports/jacoco/test/*.xml')],
                    sourceFileResolver: sourceFiles('NEVER_STORE')

            }
        }
       
    }
}
