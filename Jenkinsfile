pipeline {
    options {
        gitLabConnection('git.geogebra.org')
    }
    agent any
    stages {
        stage('build') {
            steps {
                updateGitlabCommitStatus name: 'build', state: 'pending'
                writeFile file: 'changes.csv', text: getChangelog()
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
