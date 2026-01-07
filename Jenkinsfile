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
                        env.ERROR_SUMMARY = "Compilation Failed: ${err.getMessage()}"
                        env.FAILED_STAGES = 'Compile Java Code'
                        error('Build failed during compilation')
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                // catchError sets the build to UNSTABLE if tests fail
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
                // Only attempt deploy if previous steps didn't result in FAILURE
                expression { currentBuild.result != 'FAILURE' }
            }
            steps {
                script {
                    env.DEPLOY_ATTEMPTED = 'true'
                    echo "Deploying to ${env.TARGET_ENV}"
                    try {
                        if (env.TARGET_ENV == 'DEV') {
                            sh 'exit 1' // Simulating deployment failure
                        }
                    } catch (err) {
                        env.ERROR_SUMMARY = "Deployment to ${env.TARGET_ENV} failed"
                        error "Deployment Failed"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // 1. Determine the status
                def finalResult = currentBuild.result ?: 'SUCCESS'
                
                // 2. Capture failed stages for UNSTABLE scenarios
                // We check if the quality gates or tests marked the build unstable
                def stagesThatFailed = []
                if (env.FAILED_STAGES) { stagesThatFailed << env.FAILED_STAGES }
                
                // 3. Logic for Failure Type Classification
                if (finalResult == 'UNSTABLE') {
                    env.FAILURE_TYPE = 'BUILD_UNSTABLE'
                    // Optionally collect which specific quality gate failed
                    if (env.FAILED_STAGES == '') { env.FAILED_STAGES = 'Quality Gate / Unit Tests' }
                } 
                else if (finalResult == 'FAILURE') {
                    if (env.DEPLOY_ATTEMPTED == 'true') {
                        env.FAILURE_TYPE = 'PIPELINE_FAILED'
                        env.FAILED_STAGES = 'DEPLOY'
                    } else {
                        env.FAILURE_TYPE = 'BUILD_FAILED'
                        // env.FAILED_STAGES is likely set in the Compile stage catch block
                    }
                }

                // Metadata preparation
                def startTime = new Date(currentBuild.startTimeInMillis).toString()
                def endTime = new Date().toString()
                def triggeredBy = currentBuild.getBuildCauses().collect { it.shortDescription }.join(', ')
                def changedFiles = []
                currentBuild.changeSets.each { cs ->
                    cs.items.each { item ->
                        item.affectedFiles.each { f -> changedFiles << f.path }
                    }
                }

                def payload = [
                    source       : 'jenkins',
                    job          : env.JOB_NAME,
                    buildNumber  : env.BUILD_NUMBER,
                    result       : finalResult,
                    failureType  : env.FAILURE_TYPE,
                    failedStages : env.FAILED_STAGES,
                    errorSummary : env.ERROR_SUMMARY,
                    changedFiles : changedFiles.unique(),
                    environment  : env.TARGET_ENV,
                    triggeredBy  : triggeredBy,
                    startTime    : startTime,
                    endTime      : endTime
                ]

                def payloadJson = groovy.json.JsonOutput.toJson(payload)
                echo "===== WEBHOOK PAYLOAD ====="
                echo groovy.json.JsonOutput.prettyPrint(payloadJson)
                
                sh "echo '${payloadJson}' > payload.json"
                sh "curl -X POST -H 'Content-Type: application/json' -d @payload.json https://webhook.site/4746df80-50b3-4fc8-af8f-92be5b1a512c"
            }
        }
    }
}
