# SIGEC - Système Intégré de Gestion des Candidatures

## 🚀 Démarrage Rapide

### Prérequis
- Java 17+
- PostgreSQL 12+
- Node.js 18+ (pour le frontend)
- Maven 3.8+

### Configuration

1. **Copier le fichier d'environnement**
```bash
cp .env.example .env
```

2. **Configurer les variables d'environnement**
```bash
# Base de données
DB_URL=jdbc:postgresql://localhost:5432/gestion_inscription
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_super_secret_jwt_key_minimum_256_bits
JWT_EXPIRATION=86400000

# Email
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

3. **Démarrer l'application**
```bash
# Backend
mvn spring-boot:run

# Frontend
cd frontendGI
npm install
ng serve
```

## 🏗️ Architecture

### Backend (Spring Boot)
- **Contrôleurs** : Gestion des endpoints REST
- **Services** : Logique métier
- **Repositories** : Accès aux données
- **Security** : JWT + OAuth2 + RBAC
- **Exception Handling** : Gestion centralisée des erreurs

### Frontend (Angular)
- **Components** : Interface utilisateur
- **Services** : Communication avec l'API
- **Guards** : Protection des routes
- **Interceptors** : Gestion automatique des tokens

## 🔒 Sécurité

### Authentification
- **JWT** avec expiration configurable
- **OAuth2** (Google, Microsoft)
- **reCAPTCHA** pour la protection anti-bot

### Autorisation
- **RBAC** : CANDIDATE, AGENT, SUPER_ADMIN
- **Guards** côté frontend
- **@PreAuthorize** côté backend

### Bonnes Pratiques Appliquées
- ✅ Variables d'environnement pour les secrets
- ✅ Validation Bean avec annotations
- ✅ Gestion d'erreurs globale
- ✅ Logging structuré (SLF4J)
- ✅ Profils séparés (dev/prod)
- ✅ Tests unitaires
- ✅ Monitoring avec Actuator

## 📊 Monitoring

### Endpoints Actuator
- `/actuator/health` - État de l'application
- `/actuator/info` - Informations système
- `/actuator/metrics` - Métriques de performance

### Logs
```bash
# Développement
logging.level.com.groupe.gestin_inscription=DEBUG

# Production
logging.level.root=INFO
```

## 🧪 Tests

### Exécuter les tests
```bash
mvn test
```

### Structure des tests
```
src/test/java/
├── services/          # Tests unitaires des services
├── controllers/       # Tests d'intégration des contrôleurs
└── security/         # Tests de sécurité
```

## 🚀 Déploiement

### Profils
- **dev** : Développement local
- **prod** : Production

### Variables d'environnement Production
```bash
SPRING_PROFILES_ACTIVE=prod
DB_PASSWORD=secure_production_password
JWT_SECRET=super_secure_production_jwt_key
SSL_ENABLED=true
```

## 📝 API Documentation

Swagger UI disponible sur : `http://localhost:8086/swagger-ui.html`

## 🔧 Configuration Avancée

### Base de données
```properties
# Développement
spring.jpa.hibernate.ddl-auto=create-drop

# Production
spring.jpa.hibernate.ddl-auto=validate
```

### CORS
```properties
# Développement
app.cors.allowed-origins=http://localhost:4200

# Production
app.cors.allowed-origins=https://yourdomain.com
```

## 🤝 Contribution

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit les changes (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.