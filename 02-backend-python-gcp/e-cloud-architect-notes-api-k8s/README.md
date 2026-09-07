# Proyek Notes API - Menjadi Google Cloud Architect (Dicoding)

Repository ini berisi source code dan manifest Kubernetes untuk proyek submission **Menjadi Google Cloud Architect** (Dicoding Academy).

## Arsitektur dan Spesifikasi Deployment

- **Cloud Provider:** Google Cloud Platform (GCP)
- **Project ID:** `project-bbbe31b4-c727-4c5a-96a`
- **Region / Zone:** `asia-southeast2` (Jakarta) / `asia-southeast2-a`
- **Artifact Registry Repository:** `notes-api-repo`
  - Image Tag: `asia-southeast2-docker.pkg.dev/project-bbbe31b4-c727-4c5a-96a/notes-api-repo/notes-api:v1`
  - Architecture: `linux/amd64`
- **GKE Cluster:** `notes-api-cluster`
  - Mode: Standard Cluster
  - Machine Type: `e2-micro` (Hemat resource dan cost-efficient)
  - Number of Nodes: 1 (Zonal single-node di Jakarta `asia-southeast2-a`)
- **Kubernetes Workload dan Service:**
  - Deployment: `notes-api-deployment` (1 replica)
  - Service: `notes-api-service` (Type: `LoadBalancer`)
  - External IP: `34.50.98.82`
  - Port Mapping: Port External `80` -> Container Target Port `5000` (Port mapping beda port terpenuhi)

## Endpoint dan Integrasi Front-End

- **External Endpoint Notes API:** `http://34.50.98.82` (Port 80 HTTP)
- **Front-End URL:** `http://notesapp-v1.dicodingacademy.com/`
- **Cara Integrasi:**
  1. Buka aplikasi front-end di browser (disarankan Firefox / Non-Chromium karena protokol HTTP).
  2. Klik tombol **Change URL**.
  3. Masukkan URL: `http://34.50.98.82`
  4. Aplikasi Notes App siap digunakan untuk membuat, membaca, dan menghapus catatan.

## Hak Akses Auditor Eksternal (Principle of Least Privilege)

Hak akses telah diberikan kepada tim reviewer Dicoding (`group:reviewer_googlecloud@dicoding.com`) dengan prinsip *least privilege*:
- `roles/artifactregistry.reader`: Hanya membaca container image di Artifact Registry.
- `roles/container.viewer`: Hanya melihat konfigurasi cluster dan workload GKE.
- `roles/browser`: Menjelajahi resource project di GCP Console tanpa izin modifikasi.

## Struktur Direktori

```text
.
├── Dockerfile              # Dockerfile aplikasi Notes API
├── package.json            # Node.js dependencies
├── server.js               # Entry point Hapi.js server
├── routes.js               # Routing API
├── handler.js              # Request handler
├── notes.js                # In-memory storage data notes
└── k8s/
    ├── deployment.yaml     # Kubernetes Deployment manifest
    └── service.yaml        # Kubernetes LoadBalancer Service manifest
```
