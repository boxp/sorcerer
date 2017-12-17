# puppeteer

A Slackbot for Deploying microservices to GKE Cluster.

## Prerequisites

- Leiningen(https://leiningen.org/)

### Optionals(For GKE Cluster)

- Google Cloud SDK(https://cloud.google.com/sdk/)
- Your GCP Account
- Your CircleCI Account

## Usage(Local Environment)

```sh
lein run
```

## Deploy to GKE Cluster

1. Add Integration with CircleCI to your GitHub repository.
2. Add your Google Application Credentials as `$ACCT_AUTH`.
3. Apply Kubernetes/Secret to your Cluster(Examples below).

```yml
apiVersion: v1
kind: Secret
metadata:
  name: puppeteer
type: Opaque
data:
  slack-token: <echo -n "[SLACK_TOKEN]" | base64>
  github-oauth-token: <echo -n "[GITHUB_OAUTH_TOKEN]" | base64>
  aws-access-key: <echo -n "[AWS_ACCESS_KEY]" | base64>
  aws-secret-key: <echo -n "[AWS_SECRET_KEY]" | base64>
  dynamodb-endpoint: <echo -n "http://localhost:8001" | base64>
  k8s-domain: <echo -n "boxp-tk" | base64>
  k8s-ingress-name: <echo -n "shanghai" | base64>
  dns-zone: <echo -n "boxp-tk" | base64>
```

4. Just `git push` commits.

## License

Copyright © 2017 boxp

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
