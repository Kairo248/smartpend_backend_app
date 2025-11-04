# SpendSmart Backend

A Spring Boot REST API for personal finance management.

## 🚀 Features

- **Authentication & Authorization**: JWT-based security
- **User Management**: Registration, login, profile management
- **Expense Tracking**: CRUD operations for income/expenses
- **Category Management**: Custom and system categories
- **Wallet Management**: Multiple wallets with different currencies
- **Budget Management**: Set and track budgets by category or overall
- **Analytics**: Spending trends, category breakdowns, dashboard summaries
- **RESTful API**: Comprehensive REST endpoints

## 🛠 Tech Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **Spring Security** with JWT
- **Spring Data JPA**
- **H2/PostgreSQL Database**
- **Maven** for dependency management
- **Lombok** for reducing boilerplate code
- **MapStruct** for object mapping

## 🏗 Architecture

```
src/
├── main/
│   ├── java/com/spendSmart/backend/
│   │   ├── config/         # Security, CORS configuration
│   │   ├── controller/     # REST endpoints
│   │   ├── dto/           # Data Transfer Objects
│   │   ├── entity/        # JPA entities
│   │   ├── repository/    # Data access layer
│   │   ├── security/      # JWT, UserPrincipal
│   │   ├── service/       # Business logic
│   │   └── BackendApplication.java
│   └── resources/
│       └── application.properties
└── test/
```

## 📋 API Endpoints

### Authentication
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh JWT token
- `POST /api/v1/auth/logout` - User logout
- `GET /api/v1/auth/me` - Get current user

### Expenses
- `GET /api/v1/expenses` - Get user expenses
- `POST /api/v1/expenses` - Create new expense
- `GET /api/v1/expenses/{id}` - Get expense details
- `PUT /api/v1/expenses/{id}` - Update expense
- `DELETE /api/v1/expenses/{id}` - Delete expense

### Categories
- `GET /api/v1/categories` - Get user categories
- `POST /api/v1/categories` - Create category
- `PUT /api/v1/categories/{id}` - Update category
- `DELETE /api/v1/categories/{id}` - Delete category

### Wallets
- `GET /api/v1/wallets` - Get user wallets
- `POST /api/v1/wallets` - Create wallet
- `PUT /api/v1/wallets/{id}` - Update wallet
- `DELETE /api/v1/wallets/{id}` - Delete wallet

### Budgets
- `GET /api/v1/budgets` - Get user budgets
- `POST /api/v1/budgets` - Create budget
- `PUT /api/v1/budgets/{id}` - Update budget
- `DELETE /api/v1/budgets/{id}` - Delete budget

### Analytics
- `GET /api/v1/analytics/dashboard` - Dashboard summary
- `GET /api/v1/analytics/expenses` - Expense analytics
- `GET /api/v1/analytics/trends` - Spending trends

## 🚦 Getting Started

### Prerequisites
- Java 21+
- Maven 3.6+

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd spend-smart-backend
```

2. **Configure database** (Optional - uses H2 by default)
```properties
# application.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
```

3. **Build and run**
```bash
# Using Maven wrapper
./mvnw clean install
./mvnw spring-boot:run

# Or using Maven
mvn clean install
mvn spring-boot:run
```

4. **Access the API**
- Base URL: `http://localhost:8080/api/v1`
- H2 Console: `http://localhost:8080/h2-console` (if using H2)

## 🔒 Security

- JWT-based authentication
- Password encryption with BCrypt
- CORS configuration for frontend integration
- Protected endpoints with role-based access

## 📊 Database Schema

### Core Entities
- **User**: User accounts and profiles
- **Category**: Expense/income categories
- **Wallet**: User wallets/accounts
- **Expense**: Transactions (income/expenses)
- **Budget**: Budget tracking
- **RefreshToken**: JWT refresh tokens

### Relationships
- User → Many Categories, Wallets, Expenses, Budgets
- Expense → One Category, One Wallet
- Budget → One Category (optional for overall budget)

## 🧪 Testing

```bash
# Run tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## 📝 Configuration

### Application Properties
```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=update

# JWT Configuration
app.jwtSecret=mySecretKey
app.jwtExpirationInMs=86400000
app.jwtRefreshExpirationInMs=604800000

# CORS Configuration
app.cors.allowedOrigins=http://localhost:3000
```

## 🔧 Development

### Code Style
- Follow Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Implement proper exception handling
- Write comprehensive unit tests

### Adding New Features
1. Create entity in `entity/` package
2. Add repository in `repository/` package
3. Create DTOs in `dto/` package
4. Implement service in `service/` package
5. Add controller in `controller/` package
6. Write tests

## 📈 Performance

- Connection pooling configured
- JPA query optimization
- Proper indexing on database
- Caching for frequently accessed data

## 🚀 Deployment

### Docker (Optional)
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/spend-smart-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables
```bash
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=your-database-url
JWT_SECRET=your-jwt-secret
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

If you have any questions or issues, please open an issue on GitHub.

---

**Happy coding! 💻✨**