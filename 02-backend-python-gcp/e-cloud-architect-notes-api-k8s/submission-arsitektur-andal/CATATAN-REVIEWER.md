# Catatan untuk Reviewer — Proyek Membangun Arsitektur yang Andal

## Akses Cepat

| Item | Nilai |
|---|---|
| **IP address aplikasi** | `8.232.179.128` (HTTP, port 80) |
| **Project ID** | `project-183cf8ce-97a2-455e-aad` |
| **Project name** | `submission-gca-arirahmatr` |
| **Pricing Calculator** | https://cloud.google.com/products/calculator/estimate-preview/CiQ5ZGVlYWIzYS00ZmRkLTRjYjUtYmNiNy1kMTc1MDE0ZjNkMGMQAQ%3D%3D |
| **Estimasi biaya** | $57.78 / bulan |

Aplikasi e-commerce di-deploy memakai startup script yang ditentukan pada dokumen
submission: `gs://cloud-training/gcpnet/httplb/startup.sh`.

## Ringkasan Pemenuhan Kriteria Wajib

| # | Kriteria | Implementasi |
|---|----------|--------------|
| 1 | Diagram rancangan arsitektur | `diagram-arsitektur.png` (+ sumber `.svg`) memuat lokasi pengguna, VPC, region, subnet + IP range, zone, nama MIG/template, IP internal tiap instance, dan alur traffic |
| 2 | Project baru sesuai format | Project name `submission-gca-arirahmatr`, bukti pada `screenshots/1_project_id.png` |
| 3 | Load balancer HTTP + 2 instance group di 2 region | Global External HTTP LB `ecommerce-http-lb-rule` (protokol HTTP, port 80) dengan backend `mig-asia` (asia-southeast2-a) dan `mig-eu` (europe-west1-b), keduanya autoscaling aktif |
| 4 | Custom dashboard pemantauan | Dashboard bernama persis `Dashboard - Submission`, berisi 4 chart dengan 4 metrik berbeda dan tipe chart bervariasi (3 Line + 1 Stacked Bar) |
| 5 | Hak akses auditor eksternal | `reviewer_googlecloud@dicoding.com` diberi akses pada level project |
| 6 | Perhitungan biaya arsitektur | Estimasi Pricing Calculator mencakup 2 VM (beserta ephemeral public IP) dan Cloud Load Balancing di kedua region |

## Detail Arsitektur

**Jaringan** — Custom VPC `vpc-ecommerce` (mode Custom), dengan dua subnet:

| Subnet | Region | IP range |
|---|---|---|
| `subnet-asia-southeast2` | asia-southeast2 (Jakarta) | 10.10.0.0/24 |
| `subnet-europe-west1` | europe-west1 (Belgium) | 10.20.0.0/24 |

Firewall: `allow-http-traffic` (0.0.0.0/0 → tcp:80) dan `allow-health-check`
(130.211.0.0/22, 35.191.0.0/16 → tcp:80).

**Compute & Autoscaling** — Kedua managed instance group memakai machine type
`e2-micro` dengan kebijakan autoscaling identik: minimum 1 replica, maksimum 2
replica, target CPU utilization 80%.

| MIG | Zone | Instance aktif | Internal IP |
|---|---|---|---|
| `mig-asia` | asia-southeast2-a | `mig-asia-cptq` | 10.10.0.2 |
| `mig-eu` | europe-west1-b | `mig-eu-frrl` | 10.20.0.3 |

**Load Balancing** — Forwarding rule `ecommerce-http-lb-rule` (IP Anycast
`8.232.179.128`, port 80) → target proxy `ecommerce-http-proxy` → URL map
`ecommerce-url-map` → backend service `ecommerce-backend-service` (protokol HTTP,
health check `http-basic-check`). Kedua backend berstatus **HEALTHY**.

## Penerapan Saran (Kriteria Opsional)

| Saran | Penerapan | Berkas bukti |
|---|---|---|
| Diagram arsitektur lebih detail | Lokasi pengguna, nama VPC, region, subnet + IP range, zone, dan IP address tiap instance dicantumkan | `diagram-arsitektur.png` |
| Custom VPC | `vpc-ecommerce` mode Custom dengan subnet yang dirancang per region | `screenshots/5_vpc_custom_mode.png` |
| Metrik sesuai kebutuhan + penjelasan fungsinya | 4 metrik dijelaskan pada bagian "Fungsi/Kegunaan dari Setiap Metric" | `config.txt` |
| Principle of least privilege | Reviewer hanya diberi role viewer granular: `roles/browser`, `roles/compute.viewer`, `roles/monitoring.viewer` — tanpa role Editor/Viewer luas | `screenshots/4_iam_reviewer.png` |
| Alasan pemilihan machine type, region, dan tipe load balancer | Ditulis pada bagian "Alasan Pemilihan Machine Type, Region, dan Load Balancer Type" | `config.txt` |

Saran penggunaan HTTPS pada load balancer tidak diterapkan karena kriteria wajib
menetapkan load balancer menerima protokol HTTP, dan penerbitan sertifikat
memerlukan domain berbayar di luar cakupan proyek ini.

## Isi Berkas ZIP

```
submission-arsitektur-andal/
├── config.txt
├── CATATAN-REVIEWER.md
├── diagram-arsitektur.png
├── diagram-arsitektur.svg
└── screenshots/
    ├── 1_project_id.png
    ├── 2a_load_balancer.png
    ├── 2b_managed_instance_groups.png
    ├── 3_custom_dashboard.png
    ├── 4_iam_reviewer.png
    └── 5_vpc_custom_mode.png
```

Terima kasih atas waktu dan masukannya.
