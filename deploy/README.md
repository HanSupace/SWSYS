# Tailscale SSH deployment

This project can be deployed by building the Spring Boot jar locally, uploading it through Tailscale SSH, and restarting a `systemd` service on the server.

## 1. Prepare the server once

Install Java 25 or a compatible JDK for this project. The local Windows machine also needs Java available through `JAVA_HOME` or `PATH` because the deploy script builds the jar before uploading it.

```bash
sudo useradd --system --home /opt/lastsys --shell /usr/sbin/nologin lastsys
sudo mkdir -p /opt/lastsys
sudo chown -R lastsys:lastsys /opt/lastsys
```

Copy `deploy/lastsys.service` to the server:

```bash
sudo cp lastsys.service /etc/systemd/system/lastsys.service
sudo systemctl daemon-reload
sudo systemctl enable lastsys
```

Edit `/etc/systemd/system/lastsys.service` and set the production database values:

```ini
Environment=SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/lastsys?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
Environment=SPRING_DATASOURCE_USERNAME=lastsys
Environment=SPRING_DATASOURCE_PASSWORD=change-me
```

## 2. Enable Tailscale SSH

On the server:

```bash
sudo tailscale up --ssh
tailscale status
```

Make sure your local Windows machine can SSH to the Tailscale machine name or 100.x address:

```powershell
ssh your-user@your-server
```

For the school VM format, use your student number as the SSH user:

```powershell
ssh 2243428@your-tailscale-ip
```

If this is the first login and the initial password is also your student number, change it immediately after login:

```bash
passwd
```

## 3. Deploy from Windows

From the project root:

```powershell
.\deploy\tailscale-deploy.ps1 -HostName your-server -User your-user
```

`your-server` can be a Tailscale machine name, MagicDNS name, or 100.x Tailscale IP address.

For your student-number account:

```powershell
.\deploy\tailscale-deploy.ps1 -HostName your-tailscale-ip -User 2243428
```

To run tests before packaging:

```powershell
.\deploy\tailscale-deploy.ps1 -HostName your-server -User your-user -RunTests
```

The script uploads the jar to `/tmp`, installs it as `/opt/lastsys/app.jar`, restarts `lastsys`, and prints the service status.
