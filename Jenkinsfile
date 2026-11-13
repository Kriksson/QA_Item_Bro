pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 40, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'THREADS', choices: ['1', '2', '3', '4'], description: 'Количество параллельных процессов. У каждого свой стенд и база.')
        string(name: 'APP_IMAGE', defaultValue: 'item-bro-app:latest', description: 'Образ бэкенда в Docker на машине Jenkins')
        string(name: 'FRONTEND_IMAGE', defaultValue: 'item-bro-ui-frontend:latest', description: 'Образ фронта в Docker на машине Jenkins')
        string(name: 'DB_USER', defaultValue: 'kriksson', description: 'Пользователь временных тестовых баз')
        password(name: 'DB_PASSWORD', defaultValue: 'kriksson', description: 'Пароль временных тестовых баз')
        string(name: 'SELENIDE_REMOTE', defaultValue: 'http://selenoid:4444/wd/hub', description: 'Удалённый WebDriver')
    }

    environment {
        JAVA_HOME = '/opt/jdk-27'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        SELENIDE_HEADLESS = 'true'
        SELENIDE_BROWSER_VERSION = '128.0'
        ITEM_BRO_NETWORK = 'item-bro_default'
    }

    stages {
        stage('Получить исходники') {
            steps {
                git branch: 'master', url: 'https://github.com/Kriksson/QA_Item_Bro'
                script {
                    env.CI_PREFIX = "qa-${env.JOB_BASE_NAME.take(24)}-${env.BUILD_NUMBER}".toLowerCase().replaceAll('[^a-z0-9-]', '-')
                    env.API_URL = "http://${env.CI_PREFIX}-app-{worker}:8080"
                    env.UI_URL = "http://${env.CI_PREFIX}-frontend-{worker}"
                    env.DB_URL = "jdbc:postgresql://${env.CI_PREFIX}-db-{worker}:5432/item_bro"
                }
            }
        }

        stage('Очистить результаты прошлого запуска') {
            steps {
                dir('target') { deleteDir() }
                dir('allure-results') { deleteDir() }
                dir('build/reports/tests') { deleteDir() }
            }
        }

        stage('Проверить инструменты') {
            steps {
                sh '''
                    set -eu
                    java -version
                    ./mvnw -version
                    docker version
                    docker compose version
                    docker image inspect "$APP_IMAGE" "$FRONTEND_IMAGE" > /dev/null
                    remote_base=${SELENIDE_REMOTE%/}
                    remote_base=${remote_base%/wd/hub}
                    curl --fail --silent --show-error --max-time 10 "$remote_base/status"
                '''
            }
        }

        stage('Поднять отдельные стенды') {
            steps {
                sh 'bash ci/workers.sh up'
            }
        }

        stage('Запустить автотесты') {
            steps {
                sh './mvnw -B -Dtest.threads="$THREADS" test'
            }
        }
    }

    post {
        always {
            script {
                if (env.CI_PREFIX) {
                    sh returnStatus: true, script: 'bash ci/workers.sh logs > ci-services.log 2>&1'
                    def cleanup = sh returnStatus: true, script: 'bash ci/workers.sh down'
                    if (cleanup != 0) {
                        unstable('Не удалось удалить временные тестовые стенды; см. журнал сборки')
                    }
                }
            }
            junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'allure-results/**,build/reports/tests/**,ci-services.log', allowEmptyArchive: true
            allure(
                allureVersion: '3',
                includeProperties: true,
                results: [[path: 'allure-results']]
            )
        }
    }
}
