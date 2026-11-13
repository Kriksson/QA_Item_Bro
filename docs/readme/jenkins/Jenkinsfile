pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 40, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'TEST_SUITE', choices: ['ALL', 'API', 'UI'], description: 'Какие тесты запустить')
        choice(name: 'THREADS', choices: ['1', '2', '3', '4'], description: 'Количество параллельных процессов.')
    }

    environment {
        JAVA_HOME = '/opt/jdk-27'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        APP_IMAGE = 'item-bro-app:latest'
        FRONTEND_IMAGE = 'item-bro-ui-frontend:latest'
        DB_USER = 'kriksson'
        DB_PASSWORD = 'kriksson'
        SELENIDE_REMOTE = 'http://selenoid:4444/wd/hub'
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
                sh '''
                    set -eu
                    case "$TEST_SUITE" in
                        ALL)
                            ./mvnw -B -Dtest.threads="$THREADS" test
                            ;;
                        API|UI)
                            ./mvnw -B -Dtest.threads="$THREADS" "-Dtest=$TEST_SUITE.*Test" test
                            ;;
                        *)
                            echo 'TEST_SUITE должен быть ALL, API или UI' >&2
                            exit 2
                            ;;
                    esac
                '''
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
