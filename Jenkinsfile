pipeline {
    agent any

    // Ensure Maven installed via Homebrew is in PATH
    environment {
        PATH = "/opt/homebrew/bin:${env.PATH}"
    }

    stages {

        stage('Checkout') {
            steps {
                echo "🧾 Checking out code from ${env.GIT_URL}"
                checkout scm
            }
        }

        stage('Validate Environment') {
            steps {
                echo "🔍 Validating tools"
                sh 'mvn -version'
            }
        }

        stage('Compile Java Code') {
            steps {
                script {
                    try {
                        dir('Test') {
                            sh 'mvn clean compile'
                        }
                        echo "✅ Compilation successful"
                    } catch (err) {
                        echo "❌ Compilation failed: ${err.getMessage()}"
                        currentBuild.result = 'FAILURE'
                        // Store error message for webhook
                        env.COMPILE_ERROR = err.getMessage()
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "📤 Preparing webhook payload"

                def startTime = new Date(currentBuild.startTimeInMillis).toString()
                def triggeredBy = currentBuild.getBuildCauses()
                    .collect { it.shortDescription }
                    .join(", ")

                def gitBranch = env.GIT_BRANCH ?: 'unknown'
                def repoUrl = scm.userRemoteConfigs[0].url
                def repoName = repoUrl.tokenize('/').last().replace('.git', '')

                def changes = []
                currentBuild.changeSets.each { changeSet ->
                    changeSet.items.each { entry ->
                        changes << [
                            commitId: entry.commitId,
                            author  : entry.author.fullName,
                            message : entry.msg,
                            timestamp: new Date(entry.timestamp).toString(),
                            files   : entry.affectedFiles.collect { it.path }
                        ]
                    }
                }

                // Build payload
                def payload = [
                    source      : "jenkins",
                    sourceType  : "pipeline",
                    job         : env.JOB_NAME,
                    build       : env.BUILD_NUMBER,
                    status      : currentBuild.currentResult,
                    repository  : repoName,
                    repoUrl     : repoUrl,
                    branch      : gitBranch,
                    triggeredBy : triggeredBy,
                    buildStart  : startTime,
                    compileError: env.COMPILE_ERROR ?: '',
                    changes     : changes
                ]

                echo "===== WEBHOOK PAYLOAD ====="
                echo groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(payload))
                echo "==========================="

                // Convert payload to JSON string
                def payloadJson = groovy.json.JsonOutput.toJson(payload)
                
                // Write JSON to file and send using curl
                sh """
                  echo '${payloadJson}' > payload.json
                  curl -X POST \
                       -H "Content-Type: application/json" \
                       -d @payload.json \
                       -H "jenkins-token: now_KWFMnVwcWX4rR0fYr_zrqea008rWye55Oe5R9SjwPvRmUw91tw_I1ZfoRssp_1Dpk-ztEiFEFahpXmycbMuhrw" \
                       https://webhook.site/49bc75e8-2afe-460c-8382-9cbf9be8ea84
                """
            }
        }
    }
}
