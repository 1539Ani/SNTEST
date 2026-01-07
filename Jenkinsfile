pipeline {
    agent any

    environment {
        PATH = "/opt/homebrew/bin:${env.PATH}"

        DEPLOY_ATTEMPTED = 'false'
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
                        env.ERROR_SUMMARY = err.getMessage()
                        currentBuild.result = 'FAILURE'
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
                script {
                    if (currentBuild.currentResult == 'UNSTABLE') {
                        env.FAILED_STAGES += 'Unit Tests,'
                    }
                }
            }
        }

        stage('Code Coverage') {
            steps {
                dir('Test') {
                    sh 'mvn jacoco:report'
                    recordCoverage qualityGates: [
                        [metric: 'LINE', threshold: 80.0],
                        [metric: 'BRANCH', threshold: 70.0]
                    ],
                    tools: [[pattern: 'target/site/jacoco/jacoco.xml']]
                }
                script {
                    if (currentBuild.currentResult == 'UNSTABLE') {
                        env.FAILED_STAGES += 'Code Coverage,'
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
                script {
                    if (currentBuild.currentResult == 'UNSTABLE') {
                        env.FAILED_STAGES += 'Warnings,'
                    }
                }
            }
        }

        stage('DEPLOY') {
            when {
                expression { currentBuild.currentResult != 'FAILURE' }
            }
            steps {
                script {
                    env.DEPLOY_ATTEMPTED = 'true'
                }
        
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    script {
                        echo "Deploying to ${env.TARGET_ENV}"
        
                        if (env.TARGET_ENV == 'DEV') {
                            sh 'exit 1'   // simulate deployment failure
                        }
                    }
                }
        
                script {
                    if (currentBuild.currentResult == 'FAILURE') {
                        env.FAILED_STAGES = 'DEPLOY'
                        env.ERROR_SUMMARY = 'Deployment failed'
                    }
                }
            }
        }

    }

    post {
        always {
            script {
                def result = currentBuild.currentResult ?: 'SUCCESS'

                if (result == 'FAILURE') {
                    if (env.DEPLOY_ATTEMPTED == 'true') {
                        env.FAILURE_TYPE = 'PIPELINE_FAILED'
                    } else {
                        env.FAILURE_TYPE = 'BUILD_FAILED'
                    }
                } else if (result == 'UNSTABLE') {
                    env.FAILURE_TYPE = 'BUILD_UNSTABLE'
                }

                def payload = [
                    source       : 'jenkins',
                    job          : env.JOB_NAME,
                    buildNumber  : env.BUILD_NUMBER,
                    result       : result,
                    failureType  : env.FAILURE_TYPE,
                    failedStages : (env.FAILED_STAGES ?: '').replaceAll(/,$/, ''),
                    errorSummary : env.ERROR_SUMMARY ?: '',
                    environment  : env.TARGET_ENV,
                    triggeredBy  : currentBuild.getBuildCauses()
                        .collect { it.shortDescription }.join(', ')
                ]

                def payloadJson = groovy.json.JsonOutput.toJson(payload)

                echo '===== FINAL WEBHOOK PAYLOAD ====='
                echo groovy.json.JsonOutput.prettyPrint(payloadJson)
            }
        }
    }
}
