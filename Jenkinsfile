pipeline {
    agent any

    environment {
        PATH = "/opt/homebrew/bin:${env.PATH}"

        PIPELINE_STATE = 'SUCCESS'
        FAILED_STAGES  = ''
        ERROR_SUMMARY  = ''
        TARGET_ENV     = 'DEV'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* ================= BUILD ================= */
        stage('Compile Java Code') {
            steps {
                script {
                    int status = sh(
                        script: 'cd Test && mvn clean compile',
                        returnStatus: true
                    )

                    if (status != 0) {
                        env.PIPELINE_STATE = 'BUILD_FAILED'
                        env.FAILED_STAGES  = 'Compile Java Code'
                        env.ERROR_SUMMARY  = 'Compilation failed'
                        error('Stopping pipeline')
                    }
                }
            }
        }

        /* ================= TESTS ================= */
        stage('Unit Tests') {
            steps {
                script {
                    int status = sh(
                        script: 'cd Test && mvn test',
                        returnStatus: true
                    )

                    junit 'Test/target/surefire-reports/*.xml'

                    if (status != 0 && env.PIPELINE_STATE == 'SUCCESS') {
                        env.PIPELINE_STATE = 'BUILD_UNSTABLE'
                        env.FAILED_STAGES += 'Unit Tests,'
                    }
                }
            }
        }

        /* ================= COVERAGE ================= */
        stage('Code Coverage') {
            steps {
                script {
                    sh 'cd Test && mvn jacoco:report'

                    def coverageResult = recordCoverage(
                        tools: [[pattern: 'Test/target/site/jacoco/jacoco.xml']],
                        qualityGates: [
                            [metric: 'LINE', threshold: 80.0],
                            [metric: 'BRANCH', threshold: 70.0]
                        ]
                    )

                    if (currentBuild.result == 'UNSTABLE' && env.PIPELINE_STATE == 'SUCCESS') {
                        env.PIPELINE_STATE = 'BUILD_UNSTABLE'
                        env.FAILED_STAGES += 'Code Coverage,'
                    }
                }
            }
        }

        /* ================= WARNINGS ================= */
        stage('Static Analysis') {
            steps {
                script {
                    recordIssues(
                        tool: java(),
                        qualityGates: [[threshold: 10, type: 'TOTAL', unstable: true]]
                    )

                    if (currentBuild.result == 'UNSTABLE' && env.PIPELINE_STATE == 'SUCCESS') {
                        env.PIPELINE_STATE = 'BUILD_UNSTABLE'
                        env.FAILED_STAGES += 'Warnings,'
                    }
                }
            }
        }

        /* ================= DEPLOY ================= */
        stage('Deploy') {
            when {
                expression { env.PIPELINE_STATE != 'BUILD_FAILED' }
            }
            steps {
                script {
                    echo "Deploying to ${env.TARGET_ENV}"

                    int deployStatus = sh(
                        script: 'exit 1', // simulate failure
                        returnStatus: true
                    )

                    if (deployStatus != 0) {
                        env.PIPELINE_STATE = 'PIPELINE_FAILED'
                        env.FAILED_STAGES  = 'DEPLOY'
                        env.ERROR_SUMMARY  = "Deployment failed in ${env.TARGET_ENV}"
                        error('Deployment failed')
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // Map PIPELINE_STATE → Jenkins result
                if (env.PIPELINE_STATE == 'BUILD_UNSTABLE') {
                    currentBuild.result = 'UNSTABLE'
                } else if (env.PIPELINE_STATE in ['BUILD_FAILED','PIPELINE_FAILED']) {
                    currentBuild.result = 'FAILURE'
                } else {
                    currentBuild.result = 'SUCCESS'
                }

                def payload = [
                    source       : 'jenkins',
                    job          : env.JOB_NAME,
                    buildNumber  : env.BUILD_NUMBER,
                    result       : currentBuild.result,
                    failureType  : env.PIPELINE_STATE,
                    failedStages : env.FAILED_STAGES.replaceAll(/,$/, ''),
                    errorSummary : env.ERROR_SUMMARY,
                    environment  : env.TARGET_ENV,
                    triggeredBy  : currentBuild.getBuildCauses()
                        .collect { it.shortDescription }.join(', ')
                ]

                echo '===== FINAL WEBHOOK PAYLOAD ====='
                echo groovy.json.JsonOutput.prettyPrint(
                    groovy.json.JsonOutput.toJson(payload)
                )

                sh """
                  echo '${payloadJson}' > payload.json
                  curl -X POST -H "Content-Type: application/json" \
                       -d @payload.json \
                       https://webhook.site/4746df80-50b3-4fc8-af8f-92be5b1a512c
                """
            }
        }
    }
}
