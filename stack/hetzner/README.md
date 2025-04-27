# Hetzner

## Server erstellen

- Standort: Falkenstein
- Image: Apps - Docker CE
- Typ: CAX11 (ARM)
- Networking: Öffentliche IPv4 und IPv6.
- SSH-Keys: alle benötigten
- Firewalls: todo
- Backup: todo
- Cloud config:

```
#cloud-config
users:
  - name: ili2cws
    shell: /bin/bash
    groups: docker

package_upgrade: false

runcmd:
  - [su, ili2cws, -c, "git clone https://github.com/edigonzales/ili2c-web-service.git /home/ili2cws/ili2c-web-service"]
```

- Anwendung starten: todo (siehe unten)
- Floating IP: https://docs.hetzner.com/de/cloud/floating-ips/persistent-configuration/ -> todo: in cloud-config (chmod 700 /etc/netplan/60-floating-ip.yaml)

## Anwendung deployen

```
ssh root@xxxxxxxxxx
```

```
sudo su ili2cws
```

```
cd && git clone https://github.com/edigonzales/ili2c-web-service.git 
```

```
docker compose -f ili2c-web-service/stack/hetzner/docker-compose.yml -p ili2c-web-service
```
