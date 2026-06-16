# Koyeb deployment

Koyeb can deploy this Spring Boot app from GitHub using the `Dockerfile` in the project root.

## Requirements

This app needs an external MySQL database. Koyeb Free Instances do not provide persistent storage for running MySQL inside the app container, so configure a hosted MySQL database and put the connection values in Koyeb environment variables.

Required environment variables:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://HOST:3306/lastsys?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=your-db-user
SPRING_DATASOURCE_PASSWORD=your-db-password
```

## Deploy

1. Push this project to GitHub.
2. Open Koyeb and create a new Web Service.
3. Select GitHub as the source and choose this repository.
4. Choose Dockerfile build.
5. Add the environment variables above.
6. Set the service port to `8080` if Koyeb asks for a port.
7. Deploy.

Koyeb will set the public URL after the deployment becomes healthy.
