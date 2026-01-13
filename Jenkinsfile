pipeline {
    agent any

    environment {
        DEPLOY_ATTEMPTED = 'false'
        // Use locally installed Maven (Homebrew path)
        PATH = "/opt/homebrew/bin:${env.PATH}"

        FAILURE_TYPE = ''
        // Stores a short error summary, e.g., compilation or deployment errors
        ERROR_SUMMARY = ''
        // Target deployment environment (DEV / PDI / NONPROD / PROD)
        TARGET_ENV = 'PDI'
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
                expression { currentBuild.currentResult != 'FAILURE'  || currentBuild.currentResult != 'UNSTABLE'}
            }
            steps {
                // catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    script {
                        // Set flag AFTER entering catchError to persist properly
                        env.DEPLOY_ATTEMPTED = 'true'
                        echo "Deploying to ${env.TARGET_ENV}"
        
                        if (env.TARGET_ENV == 'DEV') {
                            echo "Pipeline failed in ${env.TARGET_ENV}"
                        } else {
                            echo 'Deployment successful'
                        }
                    }
                // }
            }
        }
    }

    post {
        always {
            script {
                
                def check = true
                echo "current build check ${currentBuild.currentResult} "
                echo " deploy attempted check ${env.DEPLOY_ATTEMPTED}"

                if (currentBuild.currentResult == 'FAILURE') {
                    if (env.DEPLOY_ATTEMPTED == 'true') {
                        env.FAILURE_TYPE = 'PIPELINE_FAILED'
                    } else {
                        env.FAILURE_TYPE = 'BUILD_FAILED'
                    }
                } else {
                    env.FAILURE_TYPE = 'NONE'
                }

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
                    test          : check,
                    failureType   : env.FAILURE_TYPE ?: 'NONE',
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
                    https://webhook.site/4746df80-50b3-4fc8-af8f-92be5b1a512c
                """
            }
        }
    }
}
