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
                        env.FAILED_STAGES = 'Compile Java Code'
                        env.ERROR_SUMMARY = "Compilation failed: ${err.getMessage()}"
                        error("Stopping pipeline: Build Failure")
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
                // Scenario 1 Check: If Compile failed, this stage is skipped entirely
                expression { currentBuild.result != 'FAILURE' }
            }
            steps {
                script {
                    env.DEPLOY_ATTEMPTED = 'true'
                    echo "Deploying to ${env.TARGET_ENV}"
                    try {
                        if (env.TARGET_ENV == 'DEV') {
                            // Scenario 3 Trigger: Forced failure
                            sh 'exit 1'
                        }
                    } catch (err) {
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
                // Use currentResult to capture the final outcome of the pipeline
                def buildStatus = currentBuild.currentResult ?: 'SUCCESS'
                
                // LOGIC: Classification based on your requirements
                if (buildStatus == 'FAILURE') {
                    if (env.DEPLOY_ATTEMPTED == 'true') {
                        // Scenario 3
                        env.FAILURE_TYPE = 'PIPELINE_FAILED'
                        env.FAILED_STAGES = env.FAILED_STAGES ?: 'DEPLOY'
                    } else {
                        // Scenario 1
                        env.FAILURE_TYPE = 'BUILD_FAILED'
                        env.FAILED_STAGES = env.FAILED_STAGES ?: 'Compile Java Code'
                    }
                } 
                else if (buildStatus == 'UNSTABLE') {
                    // Scenario 2
                    env.FAILURE_TYPE = 'BUILD_UNSTABLE'
                    if (!env.FAILED_STAGES) { 
                        env.FAILED_STAGES = 'Quality Gate (Coverage/Tests/Warnings)' 
                    }
                }

                // Metadata prep
                def startTime = new Date(currentBuild.startTimeInMillis).format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("IST"))
                def endTime = new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("IST"))
                def triggeredBy = currentBuild.getBuildCauses().collect { it.shortDescription }.join(', ')
                
                // Null-safe fix for the replaceAll error
                def cleanStages = (env.FAILED_STAGES ?: '').replaceAll(/,$/, '')

                def payload = [
                    source      : 'jenkins',
                    job         : env.JOB_NAME,
                    buildNumber : env.BUILD_NUMBER,
                    result      : buildStatus,
                    failureType : env.FAILURE_TYPE,
                    failedStages: cleanStages,
                    errorSummary: env.ERROR_SUMMARY ?: '',
                    environment : env.TARGET_ENV,
                    triggeredBy : triggeredBy,
                    startTime   : startTime,
                    endTime     : endTime
                ]

                def payloadJson = groovy.json.JsonOutput.toJson(payload)
                echo "===== WEBHOOK OUTPUT FOR SCENARIO ====="
                echo groovy.json.JsonOutput.prettyPrint(payloadJson)
                
                sh """
                  echo '${payloadJson}' > payload.json
                  curl -X POST -H 'Content-Type: application/json' -d @payload.json https://webhook.site/4746df80-50b3-4fc8-af8f-92be5b1a512c
                """
            }
        }
    }
}
