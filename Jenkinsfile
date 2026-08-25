// Requires an agent with Docker (with the compose v2 plugin, 2.24+ for the
// !reset/!override tags in docker-compose.ci.yml), a JDK 25 and Maven.
pipeline {
    agent any

    environment {
        // Isolates this build's containers, network and volumes from any other
        // build on the same agent, and gives `docker compose down -v` an exact
        // target to clean up.
        COMPOSE_PROJECT_NAME = "school-${env.BUILD_NUMBER}"
        COMPOSE_FILES        = "-f docker-compose.yml -f docker-compose.ci.yml"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {
        stage('Unit tests') {
            // The reactor's own tests. skipITs defaults to true in the parent
            // pom, so integration-tests contributes nothing here.
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    junit testResults: '*/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
            }
        }

        stage('Integration tests') {
            steps {
                sh '''#!/bin/bash
                set -euo pipefail

                docker compose ${COMPOSE_FILES} up --build -d

                # Compose returns as soon as the containers are created; the
                # services still have to boot and apply the Flyway migrations
                # that create the schema and the seed data the tests rely on.
                # Each service's readiness probe goes green only after that, so
                # poll all six through the proxy before running anything.
                PORT=$(docker compose ${COMPOSE_FILES} port proxy 80 | sed 's/.*://')
                BASE_URL="http://localhost:${PORT}"
                echo "Stack is up on ${BASE_URL}"

                for api in auth student faculty classes schedule register; do
                    for attempt in $(seq 1 60); do
                        if curl -fsS "${BASE_URL}/${api}-api/q/health/ready" >/dev/null 2>&1; then
                            echo "${api}-service ready"
                            break
                        fi
                        if [ "${attempt}" -eq 60 ]; then
                            echo "${api}-service never became ready" >&2
                            exit 1
                        fi
                        sleep 2
                    done
                done

                mvn -B test -pl integration-tests -DskipITs=false -Dbase.url="${BASE_URL}"
                '''
            }
            post {
                always {
                    junit testResults: 'integration-tests/target/surefire-reports/*.xml',
                          allowEmptyResults: true
                }
                failure {
                    // The tests are black-box, so a failure says almost nothing
                    // on its own -- the container logs are where the cause is.
                    sh 'docker compose ${COMPOSE_FILES} logs --no-color --tail=200 || true'
                }
            }
        }
    }

    post {
        always {
            // -v drops the postgres volume. Without it the next build reuses a
            // database that may still hold the fixed-email student the admin
            // test creates, and that test then fails on a duplicate.
            sh 'docker compose ${COMPOSE_FILES} down -v --remove-orphans || true'
        }
    }
}
