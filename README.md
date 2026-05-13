# Emerald Grove Veterinary Clinic

A comprehensive veterinary clinic management system built with Spring Boot. This application demonstrates modern Java web development practices and serves as a reference implementation for enterprise-grade applications.

## Overview

The Emerald Grove Veterinary Clinic application manages the core operations of a veterinary clinic, including:

- **Owner Management**: Register and manage pet owners with contact information
- **Pet Management**: Track pets, their types, and medical records
- **Veterinarian Management**: Manage veterinary staff and their specialties
- **Appointment System**: Schedule and track veterinary visits
- **Medical Records**: Maintain comprehensive health records for pets

## Quick Start

### Prerequisites

- **Java 17** or later
- **Maven 3.6+** or **Gradle 7+**

### Run Application

```bash
# Clone the repository (upstream)
git clone https://github.com/liatrio-labs/emerald-grove-pet-clinic
cd emerald-grove-pet-clinic

# Boeing engineers: clone from the Boeing GitLab fork instead. Replace
# the URL above with your Boeing GitLab clone URL once it exists.
# Example:
#   git clone git@gitlab.boeing.com:greenfield/emerald-grove-pet-clinic.git
#   cd emerald-grove-pet-clinic

# Run with Maven
./mvnw spring-boot:run

# Or run with Gradle
./gradlew bootRun
```

Access the application at `http://localhost:8080`

### Database Options

- **Default**: H2 in-memory database (auto-populated with sample data)
- **MySQL**: Run with `-Dspring-boot.run.profiles=mysql`
- **PostgreSQL**: Run with `-Dspring-boot.run.profiles=postgres`

## Documentation

- **[Development Guide](docs/DEVELOPMENT.md)** - Setup, testing, and contribution guidelines
- **[Architecture Guide](docs/ARCHITECTURE.md)** - System design and technical decisions
- **[Testing Guide](docs/TESTING.md)** - Comprehensive testing strategies and patterns
- **[E2E Tests (Playwright)](docs/TESTING.md#end-to-end-e2e-browser-tests-playwright)** - How to run browser-based end-to-end tests

## Development Standards

⚠️ **Important**: This project follows **Strict Test-Driven Development (TDD)** methodology. All feature implementations must follow the Red-Green-Refactor cycle:

1. **RED**: Write a failing test first
2. **GREEN**: Write minimum code to pass the test
3. **REFACTOR**: Improve code while maintaining tests

See the [Development Guide](docs/DEVELOPMENT.md) for detailed TDD requirements and development workflow.

## Technology Stack

- **Spring Boot (3.x)** - Modern Java application framework
- **Spring MVC** - Web layer with Thymeleaf templating
- **Spring Data JPA** - Data persistence layer
- **Hibernate** - ORM implementation
- **H2/MySQL/PostgreSQL** - Database options
- **Bootstrap 5** - Responsive UI framework

## Containerization

Build a Docker container image:

```bash
./mvnw spring-boot:build-image
```

## Application Features

### Core Entities

- **Owners**: Pet owners with contact details and address information
- **Pets**: Individual pets with type, birth date, and medical history
- **Vets**: Veterinary staff with specialties and contact information
- **Visits**: Appointment records with examination details
- **Specialties**: Medical specialties (Radiology, Surgery, Dentistry)

### Key Functionality

- **Owner Registration**: Add and edit pet owner information
- **Pet Management**: Register pets, track medical history
- **Veterinarian Directory**: Browse vet profiles and specialties
- **Visit Scheduling**: Book and manage veterinary appointments
- **Medical Records**: Track treatments, diagnoses, and medications

### User Interface

- **Responsive Design**: Mobile-friendly interface using Bootstrap 5
- **Intuitive Navigation**: Easy access to all major functions
- **Search & Filter**: Find owners, pets, and vets quickly
- **Form Validation**: Client-side and server-side input validation

## Configuration

### Environment Variables

Override configuration using environment variables:

```bash
export SPRING_PROFILES_ACTIVE=mysql
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/petclinic
```

### Key Configuration Files

- `application.properties` - Main application configuration
- `application-{profile}.properties` - Database-specific settings

## License

This application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).

## Origin

This repository was initially created from https://github.com/spring-projects/spring-petclinic
