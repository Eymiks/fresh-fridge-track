Démarre le serveur de développement pour ce projet.

1. Tue d'abord toutes les instances existantes sur le port 3000 :
```
netstat -ano | grep ":3000" | awk '{print $5}' | sort -u | while read pid; do taskkill //PID "$pid" //F; done
```

2. Lance ensuite le serveur avec :
```
bun run dev -- --port 3000 --host
```

Le serveur sera accessible sur https://localhost:3000 et https://192.168.1.50:3000
