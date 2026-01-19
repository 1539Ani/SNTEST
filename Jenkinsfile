def DEPLOY_ATTEMPTED = 'false'
def FAILURE_SOURCE = '' 
def FAILURE_TYPE = ''
pipeline {
    agent any

    environment {
        // Use locally installed Maven (Homebrew path)
        PATH = "/opt/homebrew/bin:${env.PATH}"

        FAILURE = ''
        // Stores a short error summary, e.g., compilation or deployment errors
        ERROR_SUMMARY = ''
        // Target deployment environment (DEV / PDI / NONPROD / PROD)
        TARGET_ENV = 'PDI'
        MAVEN_SETTINGS = "${HOME}/.m2/settings.xml" // Your local settings.xml
    }

    stages {

        /* ================= CHECKOUT ================= */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* ================= BUILD ================= */
        stage('Compile Java Code') {
            steps {
                script {
                    try {
                        dir('Test') {
                            sh 'mvn clean compile'
                        }
                    } catch (err) {
                        env.ERROR_SUMMARY = err.getMessage()
                        FAILURE_SOURCE = 'BUILD'
                        error('Build failed during compilation')
                    }
                }
            }
        }

        /* ================= UNIT TESTS ================= */
        stage('Unit Tests') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    dir('Test') {
                        sh 'mvn test'
                        junit 'target/surefire-reports/*.xml'
                    }
                }
            }
        }

        /* ================= CODE COVERAGE ================= */
        stage('Code Coverage') {
            steps {
                script {
                    dir('Test') {
                        // Generate coverage report
                        sh 'mvn jacoco:report'
        
                        // Record coverage in Jenkins
                            recordCoverage qualityGates: [[integerThreshold: 80, metric: 'LINE', threshold: 80.0], [integerThreshold: 70, metric:                             'BRANCH', threshold: 70.0]], tools: [[pattern: 'target/site/jacoco/jacoco.xml']]
                    }
                }
            }
        }
        /* ================= WARNINGS ================= */
        stage('Static Analysis (Warnings)') {
            steps {
                dir('Test') { // Ensure warnings are collected from Test module
                    recordIssues(
                        tool: java(),
                        qualityGates: [
                            [threshold: 10, type: 'TOTAL', unstable: true]
                        ]
                    )
                }
            }
        }

        /* ================= DEPLOYMENT ================= */
        stage('DEPLOY') {
             when {
                 expression { currentBuild.currentResult != 'FAILURE' && currentBuild.currentResult != 'UNSTABLE' }
             }
             steps {
                 script {
                    DEPLOY_ATTEMPTED = 'true'
                       echo "Deploying to GitHub Packages from ${env.TARGET_ENV}"

                    dir('Test') {
                        try {
                            sh "mvn deploy -s ${env.MAVEN_SETTINGS}"
                            echo 'Deployment successful'
                        } catch (err) {
                            env.ERROR_SUMMARY = err.getMessage()
                            FAILURE_SOURCE = 'PIPELINE'
                            error('Deployment failed!')
                        }
                    }
                 }
             }
        }
    }

    post {

        failure {
           script {
               echo "failure_source check ${FAILURE_SOURCE}"
               if (currentBuild.currentResult == 'FAILURE') {
                   if (FAILURE_SOURCE == 'PIPELINE') {
                       echo "inside failure_source ${FAILURE_SOURCE}"
                       FAILURE_TYPE = 'PIPELINE_FAILED'
                       env.FAILURE = FAILURE_TYPE
                       echo "failure type ${FAILURE_TYPE}"
                   } else if (FAILURE_SOURCE == 'BUILD') {
                       FAILURE_TYPE = 'BUILD_FAILED'
                   } else {
                       FAILURE_TYPE = 'UNKNOWN_FAILURE'
                   }
               } else {
                   FAILURE_TYPE = 'NONE'
               }
           }
        }

        always {
            script {

            echo "current build check ${currentBuild.currentResult} "
            echo " deploy attempted check ${DEPLOY_ATTEMPTED}"

                def startTime = new Date(currentBuild.startTimeInMillis).toString()
                def endTime = new Date().toString()
                def triggeredBy = currentBuild.getBuildCauses()
                    .collect { it.shortDescription }
                    .join(', ')

                def changedFiles = []
                currentBuild.changeSets.each { cs ->
                    cs.items.each { item ->
                        item.affectedFiles.each { f ->
                            changedFiles << f.path
                        }
                    }
                }

                def payload = [
                    source        : 'jenkins',
                    job           : env.JOB_NAME,
                    buildNumber   : env.BUILD_NUMBER,
                    result        : currentBuild.currentResult,
                    failureType   : env.FAILURE ,
                    errorSummary  : env.ERROR_SUMMARY ?: '',
                    changedFiles  : changedFiles.unique(),
                    environment   : env.TARGET_ENV ?: '',
                    triggeredBy   : triggeredBy,
                    startTime     : startTime,
                    endTime       : endTime
                ]

                def payloadJson = groovy.json.JsonOutput.toJson(payload)

                echo "===== WEBHOOK PAYLOAD ====="
                echo groovy.json.JsonOutput.prettyPrint(payloadJson)
                echo "==========================="

                sh """
                  curl -X POST \
                    -H "Content-Type: application/json" \
                    -d '${payloadJson}' \
                    https://webhook.site/5f7e1486-0835-4f4a-90a3-515d836ea7e8
                """
            }
        }
    }
}
