# Planning Model API

## Development

How to run the application in development mode.

1. Initialize docker
```bash
 colima start
```
> **Note:** How to install [docker](https://furydocs.io/container-platform/1.0.9/guide/#/install-colima)
2. Build a mysql docker image locally
```bash
make db-build
``` 
3. Run the MySQL docker image
```bash
make db-run
```
4. Run the application or tests
5. Stop the MySQL docker image
```bash
make db-stop
```
6. Stop docker
```bash
colima stop
```

> **Note:** You can access the MySQL database using the data on application-development.properties file
