# 🐳 Guide Docker - SIGEC

## 🚀 Démarrage Rapide

### Prérequis
- Docker 20.10+
- Docker Compose 2.0+

### 1. Configuration
```bash
# Copier le fichier d'environnement
cp .env.example .env

# Éditer .env avec vos vraies valeurs
nano .env
```

### 2. Lancement Complet
```bash
# Construire et démarrer tous les services
docker-compose up --build

# En arrière-plan
docker-compose up -d --build
```

### 3. Accès aux Services
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8086
- **Swagger UI**: http://localhost:8086/swagger-ui.html
- **Base de données**: localhost:5432

## 🛠️ Commandes Utiles

### Gestion des Services
```bash
# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les volumes
docker-compose down -v

# Voir les logs
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f backend
```

### Développement
```bash
# Démarrer seulement la base de données
docker-compose -f docker-compose.dev.yml up -d

# Reconstruire un service spécifique
docker-compose build backend
docker-compose up -d backend
```

### Maintenance
```bash
# Nettoyer les images inutilisées
docker system prune -a

# Voir l'utilisation des volumes
docker volume ls
```

## 🔧 Structure Docker

```
├── Dockerfile                 # Backend Spring Boot
├── frontendGI/
│   ├── Dockerfile            # Frontend Angular
│   └── nginx.conf           # Configuration Nginx
├── docker-compose.yml       # Production
├── docker-compose.dev.yml   # Développement
└── .dockerignore            # Fichiers exclus
```

## 🐛 Dépannage

### Problèmes Courants
1. **Port déjà utilisé**: Modifier les ports dans docker-compose.yml
2. **Erreur de build**: Vérifier les Dockerfile et .dockerignore
3. **Base de données**: Vérifier les variables d'environnement

### Logs de Debug
```bash
# Logs détaillés
docker-compose logs --tail=100 -f backend

# Entrer dans un conteneur
docker exec -it sigec-backend bash
```