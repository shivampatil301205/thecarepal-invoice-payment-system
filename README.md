# TheCarePal – Invoice & Online Payment System

A full-stack web application designed to simplify care-service billing, automate invoice generation, manage outstanding payments, and provide secure online payment processing through Razorpay.

The system combines a responsive React frontend with a Spring Boot REST API and MySQL database to deliver an integrated invoice and payment management solution.

## Features

- **Invoice Generation:** Create invoices using care-service dates, descriptions, and daily rates.
- **Automated Billing:** Calculate subtotals, discounts, GST, and final payable amounts.
- **PDF Invoice Generation:** Generate and download professional invoice documents.
- **Invoice Management Dashboard:** Track paid, due, and overdue invoices with payment summaries.
- **Razorpay Payment Integration:** Process payments securely using Razorpay Test Mode.
- **Backend Payment Verification:** Validate payment signatures and verify captured transactions through the Razorpay API.
- **Webhook Integration:** Receive and validate `payment.captured` notifications using HMAC-SHA256.
- **Duplicate Event Protection:** Track processed webhook event IDs to prevent repeated sequential event processing.
- **Payment Receipts:** Display transaction details, paid amounts, and outstanding invoice balances.

## Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React.js, JavaScript, CSS, Vite |
| Backend | Java 21, Spring Boot, Spring Data JPA |
| Database | MySQL |
| Payment Gateway | Razorpay |
| PDF Generation | OpenPDF |
| API Architecture | REST |
| Build Tools | Maven, npm |
| Development Tools | VS Code, Git, ngrok |

## System Architecture

The application follows a layered architecture that separates presentation, business logic, and data persistence.

```text
React Frontend
      |
      | HTTP / REST API
      v
Spring Boot Backend
      |
      |-- Invoice Controller
      |-- Invoice Service
      |-- Payment Service
      |-- Repository Layer
      |
      +--------------------+
      |                    |
      v                    v
MySQL Database       Razorpay API
                           |
                           | Payment Webhooks
                           v
                    Spring Boot Backend
```

The frontend handles user interactions, invoice creation, payment initiation, and receipt display. Spring Boot manages invoice calculations, payment verification, webhook processing, and database operations.

## Project Structure

```text
thecarepal-project/
├── backend/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/thecarepal/invoices/
│           │   ├── Application.java
│           │   ├── ApiController.java
│           │   ├── InvoiceService.java
│           │   ├── PaymentService.java
│           │   ├── Models.java
│           │   ├── Repositories.java
│           │   └── SeedAndReminder.java
│           └── resources/
│               └── application.properties
├── frontend/
│   ├── src/
│   │   ├── main.jsx
│   │   └── style.css
│   ├── package.json
│   └── vite.config.js
├── .env.example
└── README.md
```

## Prerequisites

The following software and services are required to run the application:

- Java 21
- Apache Maven
- Node.js and npm
- MySQL Server
- Razorpay Test Mode account
- ngrok for local webhook testing

## Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/shivampatil301205/thecarepal-invoice-payment-system.git
cd thecarepal-invoice-payment-system
```

### 2. Configure MySQL

Start the MySQL server and create the application database:

```sql
CREATE DATABASE IF NOT EXISTS thecarepal;
```

The Spring Boot application connects to MySQL through the configuration in `backend/src/main/resources/application.properties`.

### 3. Configure Environment Variables

Configure the database credentials and Razorpay Test Mode credentials before starting the backend.

On macOS or Linux:

```bash
export DB_USERNAME="root"
export DB_PASSWORD="YOUR_DATABASE_PASSWORD"

export RAZORPAY_KEY_ID="YOUR_TEST_KEY_ID"
export RAZORPAY_KEY_SECRET="YOUR_TEST_KEY_SECRET"
export RAZORPAY_WEBHOOK_SECRET="YOUR_WEBHOOK_SECRET"
```

The corresponding Spring Boot properties reference these environment variables:

```properties
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}

app.razorpay.key-id=${RAZORPAY_KEY_ID:}
app.razorpay.key-secret=${RAZORPAY_KEY_SECRET:}
app.razorpay.webhook-secret=${RAZORPAY_WEBHOOK_SECRET:}
```

Sensitive credentials must remain outside version-controlled source files.

### 4. Run the Spring Boot Backend

Navigate to the backend directory:

```bash
cd backend
mvn spring-boot:run
```

The backend runs at:

`http://localhost:8080`

### 5. Run the React Frontend

Open another terminal and navigate to the frontend directory from the repository root:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at:

`http://localhost:5173`

## REST API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/invoices` | Retrieve all invoices |
| GET | `/api/invoices/{id}` | Retrieve a specific invoice |
| POST | `/api/invoices` | Generate a new invoice |
| GET | `/api/invoices/{id}/pdf` | Download invoice as PDF |
| GET | `/api/invoices/{id}/payments` | Retrieve captured payments for an invoice |
| POST | `/api/payments/order` | Create a Razorpay payment order |
| POST | `/api/payments/verify` | Verify payment authenticity |
| POST | `/api/payments/webhook` | Receive Razorpay payment notifications |

## Invoice Calculation

The invoice generation process uses care-service rates, discounts, and GST to calculate the final payable amount.

**Subtotal**

Sum of all care-service charges.

**Discount**

```text
Discount Amount = Subtotal × Discount Percentage / 100
```

**Taxable Amount**

```text
Taxable Amount = Subtotal - Discount Amount
```

**GST**

```text
GST Amount = Taxable Amount × GST Percentage / 100
```

**Final Invoice Amount**

```text
Total = Taxable Amount + GST Amount
```

**Outstanding Balance**

```text
Balance = Total - Paid Amount
```

Monetary calculations use Java `BigDecimal` to support decimal precision.

## Payment Processing Workflow

The application integrates Razorpay Checkout with backend payment verification.

1. A customer selects an unpaid invoice.
2. React sends a payment-order request to the Spring Boot backend.
3. Spring Boot creates an order through the Razorpay API.
4. Razorpay Checkout allows the customer to complete the transaction.
5. The frontend submits the payment details and signature for backend verification.
6. Spring Boot validates the payment signature using HMAC-SHA256.
7. The backend confirms the payment status, order ID, and amount with Razorpay.
8. The invoice's paid amount and outstanding balance are updated.
9. A payment receipt becomes available to the customer.

### Webhook Processing

Razorpay also sends asynchronous payment notifications to the backend.

The webhook handler:

- Accepts `payment.captured` events.
- Validates the webhook signature against the original request body.
- Checks the Razorpay event ID for previously processed events.
- Matches the payment to its corresponding order.
- Validates the captured payment amount and status.
- Records the processed event ID.

Webhook event tracking supports duplicate-delivery protection.

## Local Webhook Configuration

Razorpay webhooks require a publicly accessible HTTPS endpoint.

For local development, ngrok can expose the Spring Boot server:

```bash
ngrok http 8080
```

The webhook endpoint follows this format:

```text
https://YOUR-PUBLIC-DOMAIN/api/payments/webhook
```

The Razorpay Test Mode webhook is configured to subscribe to:

```text
payment.captured
```

The webhook secret configured in Razorpay must match the backend's `RAZORPAY_WEBHOOK_SECRET` environment variable.

## Testing and Validation

The application has been tested locally for the following functionality:

| Test Scenario | Result |
|---|---|
| Invoice creation | Successful |
| Discount and GST calculations | Successful |
| PDF invoice download | Successful |
| Paid, due, and overdue invoice display | Successful |
| Razorpay Test Mode checkout | Successful |
| Backend payment verification | Successful |
| Payment receipt generation | Successful |
| Razorpay webhook event receipt | Successful |

The webhook implementation includes event-ID tracking for duplicate-event protection. Sequential replay and concurrent delivery behavior require separate validation.

All payment transactions are performed using Razorpay Test Mode. No real financial transactions are involved.

## Security

The application incorporates the following payment security measures:

- Server-side Razorpay signature verification.
- Webhook signature validation using HMAC-SHA256.
- Validation of payment amounts, order IDs, and captured status.
- Processed webhook event-ID tracking.
- Environment-based credential configuration.
- HTTPS endpoints for external webhook communication.

## Future Enhancements

- Role-based authentication and authorization.
- Advanced invoice search and filtering.
- Automated overdue-payment notifications.
- Enhanced invoice reporting and analytics.
- Automated unit and integration testing.
- Database-level concurrency controls for simultaneous webhook deliveries.
- Cloud deployment and production monitoring.

## AI Assistance Disclosure

AI-assisted tools, including ChatGPT, were used during development for architecture guidance, code generation, debugging, documentation, and understanding Spring Boot and Razorpay integration.

The application was configured, executed, and tested locally during development. AI-generated suggestions were reviewed and adapted as part of the implementation process.

## Author

**Shivam Patil**

B.Tech – Computer Science and Engineering  
VIT Bhopal University

## Disclaimer

This application was developed for educational and technical assignment purposes. Razorpay integration operates in Test Mode, and no real monetary transactions are processed.