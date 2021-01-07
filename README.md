## Installation (terminal commands)

Prerequisites:
```
npm install -g @aws-amplify/cli
amplify configure
```

Initialising app:
- `amplify init`
    - Name for project: default
    - Name for environment: `dev`
    - Default editor: `none`
    - Type of app: `android`
    - Location of res: default
    - AWS profile: yes, default
- `amplify add api`
    - Service: `GraphQL`
    - API name: default
    - Authorisation type: `API key`
    - Annotated GraphQL schema: `no`
    - Guided schema creation: `yes`
    - Describes project: `single object`
    - Want to edit: `no`
- `amplify add auth`
    - Default authentication and security: `Default configuration`
    - Sign in: `email`
    - Configure advanced: `no`
- `amplify add storage`
    - Service: `Content`
    - Resource name: default
    - Bucket name: default
    - Who should access: `Auth and guest`
    - Authenticated user access: `create/update, read, delete`
    - Guest access: `create/update, read, delete`
    - Lambda triggers: `no`
- `amplify push`
