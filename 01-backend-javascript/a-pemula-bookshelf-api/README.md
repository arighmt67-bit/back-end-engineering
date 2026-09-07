# Bookshelf API 📚

A production-ready RESTful API for managing a digital bookshelf with complete CRUD operations, query filtering, and code-style compliance. Built with **Node.js** and **Hapi Framework**, conforming to the **Dicoding Back-End Pemula** curriculum standards.

Deployed on **Google Cloud Run (Serverless Container)** in the `asia-southeast2` (Jakarta) region.

---

## 🌐 Live Demo & Endpoints

- **Live URL**: `https://bookshelf-api-419862250988.asia-southeast2.run.app`
- **Port (Local)**: `9000` (Default Dicoding test requirement)
- **Port (Container)**: `8080` (Cloud Run managed)

### Quick Health Check / Verification
```bash
curl -s https://bookshelf-api-419862250988.asia-southeast2.run.app/books
```
**Response**:
```json
{"status":"success","data":{"books":[]}}
```

---

## 🚀 Features & API Specification

### 1. Books Management (CRUD)
| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `POST` | `/books` | Add a new book to the shelf | `201 Created` / `400 Bad Request` |
| `GET` | `/books` | Retrieve all books (or filtered by queries) | `200 OK` |
| `GET` | `/books/{bookId}` | Retrieve detailed information of a specific book | `200 OK` / `404 Not Found` |
| `PUT` | `/books/{bookId}` | Update an existing book's details | `200 OK` / `400 Bad Request` / `404 Not Found` |
| `DELETE` | `/books/{bookId}` | Delete a book by its unique ID | `200 OK` / `404 Not Found` |

### 2. Optional Query Parameters (`GET /books`)
- `?name=<keyword>` : Case-insensitive search by book title (e.g. `?name=dicoding`).
- `?reading=0` or `?reading=1` : Filter by reading state (`0` for not reading, `1` for currently reading).
- `?finished=0` or `?finished=1` : Filter by completion state (`0` for unfinished, `1` for finished).

### 3. Business Logic Validation
- Rejects requests without a `name` property (`400 Bad Request`).
- Rejects requests where `readPage > pageCount` (`400 Bad Request`).
- Auto-calculates `finished` based on `pageCount === readPage`.
- Auto-generates unique 16-character IDs using `nanoid`.
- Records ISO timestamps for `insertedAt` and `updatedAt`.

---

## 🛠️ Tech Stack & Architecture

- **Runtime**: Node.js (v18 LTS / v22)
- **Framework**: [@hapi/hapi](https://hapi.dev/) (v21.4.0)
- **ID Generator**: [nanoid](https://github.com/ai/nanoid) (v3.3.12)
- **Code Linter**: ESLint with `eslint-config-dicodingacademy` (Airbnb style guide)
- **Containerization**: Docker (`node:18-alpine`)
- **Cloud Infrastructure**: Google Cloud Run (Serverless, Auto-scaling 0-2 instances)

---

## 🧪 Testing & Code Quality

### ESLint Check
Run the linter to verify consistent style formatting:
```bash
npx eslint .
```
*(Clean output, 0 errors/warnings).*

### Automated Postman / Newman Verification
The API passes **100% of all Mandatory and Optional assertions** using Newman:
```bash
newman run "Bookshelf API Test.postman_collection.json" \
  --environment "Bookshelf API Test.postman_environment.json"
```
**Results**:
- Total Requests: **32** (CRUD + Negative cases + Query filter tests)
- Total Assertions: **104**
- Failed Assertions: **0**

---

## 💻 Local Development

### Prerequisites
- Node.js >= 18.x
- npm

### Installation
```bash
git clone https://github.com/arighmt67-bit/Bookshelf-API.git
cd Bookshelf-API
npm install
```

### Running the Server
```bash
# Production mode (Port 9000)
npm run start

# Development mode (with nodemon)
npm run start-dev
```

---

## 🐳 Docker & Cloud Deployment

### Build & Run Container Locally
```bash
docker build -t bookshelf-api .
docker run -p 9000:8080 -e PORT=8080 bookshelf-api
```

### Deploy to Google Cloud Run
```bash
gcloud run deploy bookshelf-api \
  --source . \
  --region asia-southeast2 \
  --platform managed \
  --allow-unauthenticated
```

---

## 👤 Author

- **Ari Rahmat Romadhon**
- GitHub: [@arighmt67-bit](https://github.com/arighmt67-bit)
