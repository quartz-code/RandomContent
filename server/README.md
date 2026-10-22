# Свой сервер Together

Общий сервер Metrolist (`metroserverx.meowery.eu`) разрешает создавать комнаты только
приложениям из своего списка, и Pulse в него не входит. Поэтому есть свой сервер. Это
неизменённый [metroserver](https://github.com/MetrolistGroup/metroserver) (Go, GPL-3.0),
собранный с правилом, которое разрешает создавать комнаты Pulse (`ua_policy.json`).
Metrolist на нём тоже работает.

Сервер только пересылает команды: какая песня, пауза, перемотка. Музыку каждый телефон
качает сам, поэтому серверу хватает самой маленькой машины.

## Бесплатно: Render

1. Зайти на [render.com](https://render.com) через GitHub.
2. New → Blueprint → выбрать этот репозиторий. Render найдёт `render.yaml` и создаст
   сервис `pulse-together` на бесплатном тарифе.
3. Когда сборка закончится, адрес будет вида `https://pulse-together-xxxx.onrender.com`.
   В Pulse: Together → Настройки → Адрес сервера → `wss://pulse-together-xxxx.onrender.com/ws`
   (у всех, кто слушает вместе).

Бесплатный сервер засыпает после 15 минут без запросов, а просыпается до минуты. Чтобы он
не засыпал совсем, его раз в 5 минут «трогает» [UptimeRobot](https://uptimerobot.com)
(бесплатно): New monitor → HTTP(s) → `https://pulse-together-xxxx.onrender.com/health`,
интервал 5 минут. Бесплатных часов Render хватает, чтобы сервер работал весь месяц.

Если сервер всё же уснул, Pulse будит его сам, как только открыт экран Together, и пишет
«Сервер просыпается», пока ждёт.

## Всегда включён: свой VPS

Любой VPS с Docker (хватит 1 ядра и 512 МБ):

```bash
git clone <этот репозиторий> pulse && cd pulse/server
docker build -t pulse-together .
docker run -d --restart unless-stopped -p 8080:8080 -v pulse-together:/app/data --name pulse-together pulse-together
```

Для `wss://` нужен HTTPS перед сервером: например, Caddy с доменом
(`reverse_proxy localhost:8080`) — он сам получит сертификат.

## Проверка

`https://<адрес>/health` отвечает `{"status":"ok"}`.
