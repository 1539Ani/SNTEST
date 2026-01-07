pipeline {
    agent any

    environment {
        DEPLOY_ATTEMPTED = 'false'
        PATH = "/opt/homebrew/bin:${env.PATH}"
        FAILURE_TYPE = 'NONE'
        FAILED_STAGES = ''
        ERROR_SUMMARY = ''
        TARGET_ENV = 'DEV'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile Java Code') {
            steps {
                script {
                    try {
                        dir('Test') {
                            sh 'mvn clean compile'
                        }
                    } catch (err) {
                        env.FAILURE_TYPE = 'BUILD_FAILED'
                        env.FAILED_STAGES = 'Compile Java Code'
                        env.ERROR_SUMMARY = "Compilation failed: ${err.getMessage()}"
                        error("Stopping pipeline due to compilation failure")
                    }
                }
            }
        }

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

        stage('Code Coverage') {
            steps {
                script {
                    dir('Test') {
                        sh 'mvn jacoco:report'
                        // If quality gates fail, the build automatically becomes UNSTABLE
                        recordCoverage qualityGates: [
                            [integerThreshold: 80, metric: 'LINE', threshold: 80.0], 
                            [integerThreshold: 70, metric: 'BRANCH', threshold: 70.0]
                        ], tools: [[pattern: 'target/site/jacoco/jacoco.xml']]
                    }
                }
            }
        }

        stage('Static Analysis (Warnings)') {
            steps {
                dir('Test') {
                    recordIssues(
                        tool: java(),
                        qualityGates: [[threshold: 10, type: 'TOTAL', unstable: true]]
                    )
                }
            }
        }

        stage('DEPLOY') {
            when {
                // Ensure we don't deploy if a previous stage (like Compile) caused a FAILURE
                expression { currentBuild.result != 'FAILURE' }
            }
            steps {
                script {
                    env.DEPLOY_ATTEMPTED = 'true'
                    echo "Deploying to ${env.TARGET_ENV}"
                    try {
                        if (env.TARGET_ENV == 'DEV') {
                            sh 'exit 1'
                        }
                    } catch (err) {
                        env.FAILURE_TYPE = 'PIPELINE_FAILED'
                        env.FAILED_STAGES = 'DEPLOY'
                        env.ERROR_SUMMARY = "Deployment failed in ${env.TARGET_ENV}"
                        error("Deployment Failed")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // --- Status Correction Logic ---
                
                // 1. If any stage was marked UNSTABLE (tests, coverage, or warnings)
                if (currentBuild.result == 'UNSTABLE') {
                    env.FAILURE_TYPE = 'BUILD_UNSTABLE'
                    // Check coverage/tests to populate failed stages if empty
                    if (!env.FAILED_STAGES) {
                        env.FAILED_STAGES = "Quality Gate / Unit Test Failure"
                    }
                } 
                // 2. If the build FAILED (Compile or Deploy)
                else if (currentBuild.result == 'FAILURE') {
                    if (env.DEPLOY_ATTEMPTED == 'true') {
                        env.FAILURE_TYPE = 'PIPELINE_FAILED'
                        env.FAILED_STAGES = 'DEPLOY'
                    } else {
                        env.FAILURE_TYPE = 'BUILD_FAILED'
                        if (!env.FAILED_STAGES) { env.FAILED_STAGES = 'Build/Compile Stage' }
                    }
                }

                // Prepare Webhook Data
                def startTime = new Date(currentBuild.startTimeInMillis).format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("IST"))
                def endTime = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("IST"))
                def triggeredBy = currentBuild.getBuildCauses().collect { it.shortDescription }.join(', ')
                def changedFiles = []
                currentBuild.changeSets.each { cs ->
                    cs.items.each { item ->
                        item.affectedFiles.each { f -> changedFiles << f.path }
                    }
                }

                def payload = [
                    source      : 'jenkins',
                    job         : env.JOB_NAME,
                    buildNumber : env.BUILD_NUMBER,
                    result      : currentBuild.result ?: 'SUCCESS',
                    failureType : env.FAILURE_TYPE ?: 'NONE',
                    failedStages: env.FAILED_STAGES.replaceAll(/,$/, ''),
                    errorSummary: env.ERROR_SUMMARY,
                    changedFiles: changedFiles.unique(),
                    environment : env.TARGET_ENV,
                    triggeredBy : triggeredBy,
                    startTime   : startTime,
                    endTime     : endTime
                ]

                def payloadJson = groovy.json.JsonOutput.toJson(payload)
                echo "===== FINAL WEBHOOK PAYLOAD ====="
                echo groovy.json.JsonOutput.prettyPrint(payloadJson)
                
                // Execute webhook call
                sh """
                  echo '${payloadJson}' > payload.json
                  curl -X POST -H 'Content-Type: application/json' -d @payload.json https://webhook.site/4746df80-50b3-4fc8-af8f-92be5b1a512c
                """
            }
        }
    }
}
