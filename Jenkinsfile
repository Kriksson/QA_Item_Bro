pipeline {
    agent any

    environment {
        JAVA_HOME = "/opt/jdk-27"
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage("Получить исходники") {
            steps {
                git branch: 'master',
                url: 'https://github.com/Kriksson/QA_Item_Bro'
            }
        }

        stage("Проверить Java & Maven") {
            steps {
                sh 'java -version'
                sh './mvnw -version'
            }
        }

        stage("Очистить результаты прошлого запуска") {
            steps {
                sh './mvnw -B clean'
                dir('allure-results') {
                    deleteDir()
                }
            }
        }

        stage("Запустить автотесты") {
            steps {
                sh './mvnw -B test'
            }
        }
    }
    post {
        always {
            junit 'target/surefire-reports/TEST-*.xml'

            archiveArtifacts(
                    artifacts: 'allure-results/**'
            )

            allure(
                allureVersion: '3',
                    includeProperties: true,
                    results: [[path: 'allure-results']]
            )
        }
    }
}